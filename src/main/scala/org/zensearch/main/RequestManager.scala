package org.zensearch.main

import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, Props, Stash}
import akka.event.LoggingReceive
import akka.pattern.ask
import scala.concurrent.duration._

import org.zensearch.formatting.FormattingActor
import org.zensearch.main.ConfigMessages.GetTypeInfo
import org.zensearch.main.ConfigModel.TypeInfos
import org.zensearch.messages.Messages.LookupRefs
import org.zensearch.model.Model.{Request, Response}
import org.zensearch.parsing.ParserManager
import org.zensearch.parsing.ParserMessages.GetLookupRefs
import org.zensearch.request.RequestHandler
import org.zensearch.util.ChildProvider

class RequestManager(childProvider: ChildProvider) extends Actor with ActorLogging with Stash {

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

    case req: Request =>
      log.debug(s"ReqMgr - got Request: $req - stashing it")
      stash()
  }

  def waitForLookupRefs(typeInfos: TypeInfos): Receive = LoggingReceive {
    case refs: LookupRefs =>
      val formatter = childProvider.newChild(context, FormattingActor.props(refs.dataRefs, self))
      unstashAll()
      context.become(ready(typeInfos, refs, formatter))

    case req: Request =>
      log.debug(s"ReqMgr - got Request: $req - stashing it")
      stash()
  }

  def ready(typeInfos: TypeInfos, refs: LookupRefs, formatter: ActorRef): Receive = LoggingReceive {
    case req: Request =>
      val replyTo = sender()
      log.debug(s"ReqMgr - got Request: $req - processing - reply-to: $replyTo")
      (handler(typeInfos, childProvider, context, refs) ? req)
        .mapTo[Response]
        .map(formatter.tell(_, replyTo))

    case other =>
      log.error(s"Unexpected msg in RequestManager - 'ready': '$other' - sender: $sender")
  }
}

object RequestManager {
  def props(childProvider: ChildProvider) = Props(classOf[RequestManager], childProvider)

  def handler(typeInfos: TypeInfos, childProvider: ChildProvider, context: ActorContext, refs: LookupRefs) =
      childProvider.newChild(context, RequestHandler.props(typeInfos, refs, childProvider))
}
