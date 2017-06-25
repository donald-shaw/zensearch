package org.zensearch.indexing

import akka.actor.{Actor, ActorLogging, Props}
import akka.event.LoggingReceive
import spray.json.{JsArray, JsBoolean, JsNumber, JsObject, JsString}

import org.zensearch.messages.Messages.{IndexLookup, IndexResult}
import org.zensearch.model.Model.{Criteria, InternalId}

class IndexActor(criteria: Criteria, data: List[JsObject]) extends Actor with ActorLogging {

  val index = IndexActor.generateIndex(criteria, data)

  override def receive = LoggingReceive {
    case lookup: IndexLookup =>
      sender ! IndexResult(index.getOrElse(lookup.value, Nil))

    case other =>
      log.error(s"Unexpected msg in IndexActor (criteria: $criteria) - 'receive': '$other' - sender: $sender")
  }

}

object IndexActor {
  def props(criteria: Criteria, data: List[JsObject]) = Props(classOf[IndexActor], criteria, data)

  def generateIndex(criteria: Criteria, data: List[JsObject]) = {
    data.zipWithIndex.map { case (entry, id) =>
      (entry.fields.get(criteria.field) match {
        case None => None
        case Some(JsString(str)) => Some(str)
        case Some(JsNumber(num)) => Some(num.toString)
        case Some(JsBoolean(bool)) => Some(if (bool) "true" else "false")
        case Some(JsArray(entries)) => // ignore for now // @TODO - DSS: handle multiple values.
      }) -> InternalId(criteria.dtype, id)
    }.groupBy(_._1).mapValues(_.map(_._2))
  }
}
