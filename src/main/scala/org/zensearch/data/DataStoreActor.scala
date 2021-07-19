package org.zensearch.data

import akka.actor.{Actor, ActorLogging, Props}
import akka.event.LoggingReceive
import spray.json.JsObject

import org.zensearch.messages.Messages.{DataLookup, DataResult}
import org.zensearch.model.Model.{DataEntry, DataType, InternalId}

class DataStoreActor(dtype: DataType, data: Map[InternalId, JsObject]) extends Actor with ActorLogging {

  override def receive = LoggingReceive {
    case DataLookup(ids) =>
      sender ! DataResult(data.filterKeys(ids.contains).map{ case (id, js) => DataEntry(id, js)}.toList)

    case other =>
      log.error(s"Unexpected msg in DataStoreActor (data type: $dtype) - 'receive': '$other' - sender: $sender")
  }

}

object DataStoreActor {
  def props(dtype: DataType, data: Map[InternalId, JsObject]) = Props(classOf[DataStoreActor], dtype, data)
}
