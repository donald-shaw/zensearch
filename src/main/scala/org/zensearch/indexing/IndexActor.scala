package org.zensearch.indexing

import akka.actor.{Actor, ActorLogging, Props}
import akka.event.LoggingReceive
import spray.json.{JsArray, JsBoolean, JsNumber, JsObject, JsString, JsValue}

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
    
    def convert(value: Option[JsValue]): Seq[Option[String]] = value match {
      case None                  => None :: Nil
      case Some(JsString(str))   => Some(str) :: Nil
      case Some(JsNumber(num))   => Some(num.toString) :: Nil
      case Some(JsBoolean(bool)) => Some(if (bool) "true" else "false") :: Nil
      case Some(JsArray(entries)) => entries.map(Option(_)).flatMap(convert)
    }
    
    data.zipWithIndex.flatMap { case (entry, id) =>
      convert(entry.fields.get(criteria.field)).map(_ -> InternalId(criteria.dtype, id))
    }.groupBy(_._1).mapValues(_.map(_._2))
  }
}
