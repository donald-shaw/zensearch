package org.zensearch.messages

import akka.actor.ActorRef

import org.zensearch.model.Model._

object Messages {

  case class DataFiles(files: List[String])

  case class IndexRefs(refs: Map[Criteria, ActorRef])
  case object RequestIndexedFields
  case class IndexedFields(fields: Map[DataType, Set[Criteria]])
  case class DataStoreRefs(refs: Map[DataType, ActorRef])
  case class LookupRefs(indexRefs: IndexRefs, dataRefs: DataStoreRefs)

  case class IndexLookup(value: Option[String])
  case class IndexResult(ids: List[InternalId])

  case class DataLookup(ids: List[InternalId])
  case class DataResult(data: List[DataEntry])

  case class FormatResult(data: List[OutputEntry])
}
