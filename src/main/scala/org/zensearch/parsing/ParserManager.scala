package org.zensearch.parsing

import akka.actor.{Actor, ActorLogging, Props, Stash}
import akka.event.LoggingReceive
import scala.io.Source
import spray.json._

import org.zensearch.data.DataStoreActor
import org.zensearch.indexing.IndexActor
import org.zensearch.main.ConfigModel.TypeInfos
import org.zensearch.messages.Messages.{DataStoreRefs, IndexRefs, LookupRefs}
import org.zensearch.model.Model.{Criteria, DataType, InternalId}
import org.zensearch.util.ChildProvider

object ParserMessages {
  case class GetLookupRefs(typeInfo: TypeInfos)
}

class ParserManager(childProvider: ChildProvider) extends Actor with ActorLogging with Stash {

  import ParserMessages._

  def receive = LoggingReceive {
    case GetLookupRefs(typeInfos) =>
      log.debug(s"Got type info: \n${typeInfos.infos.mkString(",\n")}\n")
      val lookupRefs = processDataFiles(typeInfos)
      log.debug(s"Got lookup refs - shifting to ready state and unstashing")
      sender() ! lookupRefs
  }

  private def processDataFiles(typeInfos: TypeInfos) = {
    val refs = for {
      typeInfo <- typeInfos.infos
      dataType = DataType(typeInfo.type_name)
      _ = log.debug(s"About to read and parse file for type '${typeInfo.type_name}': '${typeInfo.file_name}'")
      json = Source.fromInputStream(getClass.getResourceAsStream(s"/${typeInfo.file_name}")).getLines().mkString("\n")
      data = json.parseJson match {
          case JsArray(vals) =>
            log.debug(s"Parsed data from file '${typeInfo.file_name}'") // - vals: ${vals.mkString("\n",",\n","\n\n")}")
            vals.toList.map(_.asJsObject)
          case _ =>
            log.error(s"Cannot parse data from file '${typeInfo.file_name}' - data doesn't read as a Json array")
            Nil // Should properly indicate error state up the line.
        }
      indexes = genIndexes(data, dataType)
      store = genDataStore(data, dataType)
    } yield (indexes, (dataType -> store))
    val (indexes, stores) = refs.unzip
    LookupRefs(IndexRefs(indexes.flatten.toMap), DataStoreRefs(stores.toMap))
  }

  private def genIndexes(data: List[JsObject], dataType: DataType) = {
    log.debug(s"Generating indexes for  '${dataType.name}'")
    val fields = data.map(_.fields.keySet).fold(Set.empty) {_ | _}
    log.debug(s"Count of fields to index: ${fields.size}")
    val result = for {
      field <- fields
      criteria = Criteria(dataType, field)
    } yield (criteria, childProvider.newChild(context, IndexActor.props(criteria, data)))
    log.debug(s"Indexing result: ${result}")
    result
  }

  private def genDataStore(data: List[JsObject], dataType: DataType) = {
    val entries = data.zipWithIndex.map{ case (entry, id) => InternalId(dataType, id) -> entry }.toMap
    childProvider.newChild(context, DataStoreActor.props(dataType, entries))
  }
}

object ParserManager {
  def props(childProvider: ChildProvider) = Props(classOf[ParserManager], childProvider)
}
