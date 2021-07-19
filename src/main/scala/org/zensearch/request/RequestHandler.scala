package org.zensearch.request

import akka.actor.{ActorContext, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import scala.concurrent.duration._
import spray.json.{JsNumber, JsString}

import org.zensearch.main.ConfigModel.{CrossLink, TypeInfos}
import org.zensearch.messages.Messages._
import org.zensearch.model.Model._
import org.zensearch.util.{ChildProvider, ShortLivedActor}


class RequestHandler(typeInfos: TypeInfos, refs: LookupRefs, childProvider: ChildProvider)
    extends ShortLivedActor with ActorLogging {

  import RequestHandler._

  val max_time_to_live = 6 seconds

  def receive = ready

  def ready: Receive = LoggingReceive {
    case req: Request =>
      val replyTo = sender()
      log.debug(s"Req Handler - got Request: $req - processing - reply-to: $replyTo")
      reqActor(childProvider, context, refs) ! req
      context.become(waitForBaseResponse(req, replyTo))

    case other =>
      log.error(s"Unexpected msg in RequestHandler - 'receive': '$other' - sender: $sender")
  }

  def waitForBaseResponse(request: Request, replyTo: ActorRef): Receive = LoggingReceive {
    case resp@Response(req, _, _, data, _) =>
      log.debug(s"ReqHandler - got base response back - resp: $resp")
      // For each result found, determine cross-linked records, and then fetch them
      val linked = findLinked(data)
      log.debug(s"ReqHandler - determined linked record requests needed: ${linked.mkString(", ")}\n\n, replyTo = $replyTo\n")
      if (linked.isEmpty) {
        replyTo ! resp // No cross-links found - can just send the response back as is
      } else {
        linked.foreach { case (_, req, _) => reqActor(childProvider, context, refs) ! req }
        context.become(waitForLinkedResponses(resp, linked, replyTo))
      }

    case other =>
      log.error(s"Unexpected msg in RequestHandler - 'waitForBaseIndexResults': '$other' - sender: $replyTo")
  }

  def waitForLinkedResponses(baseResp: Response, remaining: List[(DataEntry, Request, CrossLink)], replyTo: ActorRef,
                             returned: List[(DataEntry, Response, CrossLink)] = Nil): Receive = LoggingReceive {
    case resp: Response =>
      log.debug(s"FormattingActor - got linked Response = ${resp}\n\n, replyTo = $replyTo\n")
      remaining.find(_._2.id == resp.request.id) match {
        case Some(found) =>
          val remainder = remaining.filterNot(_ == found)
          val updated = (found._1, resp, found._3) :: returned
          if (remainder.isEmpty) {
            replyTo ! baseResp.addCrossLinks(updated)
            context.stop(self)
          } else {
            context.become(waitForLinkedResponses(baseResp, remainder, replyTo, updated))
          }
        case None => // Shouldn't happen - just ignore?
      }

    case other =>
      log.error(s"Unexpected msg in RequestActor - 'waitForDataResults': '$other' - sender: $replyTo")
  }

  private def findLinked(data: List[DataEntry]) = {
    for {
      entry <- data
      xlinkData <- typeInfos.infos.find(_.type_name == entry.id.dtype.name).map(_.cross_links).getOrElse(Nil)
      crit = Criteria(DataType(xlinkData.type_name), xlinkData.linked_field)
      xlinkRef <- entry.json.getFields(xlinkData.ref_field)
      value <- xlinkRef match {
        case JsString(str) => Some(str)
        case JsNumber(num) => Some(num.toString)
        case _ => None // No current cross-links work off any other json type
      }
    } yield (entry, Request(crit, Some(value)), xlinkData)
  }
}

object RequestHandler {
  def props(typeInfos: TypeInfos, refs: LookupRefs, childProvider: ChildProvider) = Props(classOf[RequestHandler], typeInfos, refs, childProvider)

  protected def reqActor(childProvider: ChildProvider, context: ActorContext, refs: LookupRefs) =
      childProvider.newChild(context, RequestActor.props(refs))
}
