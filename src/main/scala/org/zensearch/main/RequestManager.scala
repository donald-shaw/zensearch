package org.zensearch.main

import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, Props, Stash}
import akka.event.LoggingReceive
import akka.pattern.ask

import scala.concurrent.duration._
import org.zensearch.formatting.{DefaultFormatterFactory, Formatter, FormatterFactory}
import org.zensearch.main.ConfigMessages.GetTypeInfo
import org.zensearch.main.ConfigModel.TypeInfos
import org.zensearch.messages.Messages.{IndexedFields, LookupRefs, RequestIndexedFields}
import org.zensearch.model.Model.{Request, Response}
import org.zensearch.parsing.ParserManager
import org.zensearch.parsing.ParserMessages.GetLookupRefs
import org.zensearch.request.RequestHandler
import org.zensearch.util.ChildProvider

class RequestManager(childProvider: ChildProvider, formatterFactory: FormatterFactory) extends Actor with ActorLogging with Stash {

  import RequestManager._
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val timeout: akka.util.Timeout = 3 seconds

  private val configMgr = childProvider.newChild(context, ConfigManager.props)
  private val parser = childProvider.newChild(context, ParserManager.props(childProvider))

  override def preStart: Unit = configMgr ! GetTypeInfo
  
  def receive = waitForConfig

  def waitForConfig: Receive = LoggingReceive {
    case typeInfos: TypeInfos =>
      log.debug(s"Got type info (config): \n${typeInfos.infos.mkString(",\n")}\n")
      parser ! GetLookupRefs(typeInfos)
      context.become(waitForLookupRefs(typeInfos))
      
    case other =>
      log.debug(s"ReqMgr.waitForConfig - got message: $other - stashing it")
      stash()
  }

  def waitForLookupRefs(typeInfos: TypeInfos): Receive = LoggingReceive {
    case refs: LookupRefs =>
      val formatter: Formatter = formatterFactory() //.getOrElse(DefaultFormatterFactory)()
      unstashAll()
      context.become(ready(typeInfos, refs, formatter)) //formatActor))

    case other =>
      log.debug(s"ReqMgr.waitForLookupRefs - got message: $other - stashing it")
      stash()
  }

  def ready(typeInfos: TypeInfos, refs: LookupRefs, formatter: Formatter): Receive = LoggingReceive {
    case req: Request =>
      val replyTo = sender()
      log.debug(s"ReqMgr - got Request: $req - processing - reply-to: $replyTo")
      (handler(typeInfos, childProvider, context, refs) ? req)
        .mapTo[Response]
        .map(formatter.formatResponse(_))
        .foreach(replyTo ! _)

    case RequestIndexedFields =>
      sender() ! IndexedFields(refs.indexRefs.refs.keySet.groupBy(_.dtype))
      
    case other =>
      log.error(s"Unexpected msg in RequestManager - 'ready': '$other' - sender: $sender")
  }
}

object RequestManager {
  def props(childProvider: ChildProvider) = Props(classOf[RequestManager], childProvider, DefaultFormatterFactory)
  
  def props(childProvider: ChildProvider, formatterFactory: FormatterFactory) = Props(classOf[RequestManager], childProvider, formatterFactory)

  def handler(typeInfos: TypeInfos, childProvider: ChildProvider, context: ActorContext, refs: LookupRefs) =
      childProvider.newChild(context, RequestHandler.props(typeInfos, refs, childProvider))
}
