package org.zensearch.request

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import scala.concurrent.duration._

import org.zensearch.messages.Messages._
import org.zensearch.model.Model.{InternalId, Request, Response}
import org.zensearch.util.ShortLivedActor

class RequestActor(refs: LookupRefs) extends ShortLivedActor with ActorLogging {

  val max_time_to_live = 5 seconds

  def receive = {
    log.debug(s"ReqActor - lookup refs: ${refs.indexRefs.refs}")
    ready
  }

  def ready: Receive = LoggingReceive {
    case request@Request(_, _, criteria, value) =>
      val replyTo = sender()
      log.debug(s"ReqActor - got Request: $request - processing - reply-to: $replyTo")
      refs.indexRefs.refs.get(criteria) match {
        case Some(ref) =>
          log.debug(s"ReqActor - got ref to send lookup to: $ref")
          log.debug(s"ReqActor - ref criteria: $criteria")
          ref ! IndexLookup(value)
          context.become(waitForIndexResults(request, replyTo))
        case None =>
          log.debug(s"ReqActor - no matching ref found - refs: ${refs.indexRefs.refs}")
          replyTo ! Response(request, Nil)
      }

    case other =>
      log.error(s"Unexpected msg in RequestActor - 'receive': '$other' - sender: $sender")
  }

  def waitForIndexResults(request: Request, replyTo: ActorRef): Receive = LoggingReceive {
    case IndexResult(Nil) => replyTo ! Response(request, Nil)
    case IndexResult(ids) =>
      log.debug(s"ReqActor - got index results back - ids: ${ids.mkString(",")}")
      refs.dataRefs.refs.get(ids.head.dtype) match {
      case Some(ref) =>
        log.debug(s"ReqActor - got ref to send data-lookup to: $ref")
        ref ! DataLookup(ids)
        context.become(waitForDataResults(request, ids, replyTo))
      case None => sender() ! Response(request, Nil)
    }

    case other =>
      log.error(s"Unexpected msg in RequestActor - 'waitForIndexResults': '$other' - sender: $replyTo")
  }

  def waitForDataResults(request: Request, ids: List[InternalId], replyTo: ActorRef): Receive = LoggingReceive {
    case DataResult(Nil) =>
      replyTo ! Response(request, Nil)
      context.stop(self)

    case DataResult(data) =>
      replyTo ! Response(request, data)
      context.stop(self)

    case other =>
      log.error(s"Unexpected msg in RequestActor - 'waitForDataResults': '$other' - sender: $replyTo")
  }
}

object RequestActor {
  def props(refs: LookupRefs) = Props(classOf[RequestActor], refs)
}
