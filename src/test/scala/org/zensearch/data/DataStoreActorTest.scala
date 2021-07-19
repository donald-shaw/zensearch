package org.zensearch.data

import akka.testkit.TestActorRef
import scala.io.Source
import spray.json._

import org.zensearch.ZenSearchTestBase
import org.zensearch.messages.Messages.{DataLookup, DataResult}
import org.zensearch.model.Model.{DataType, InternalId}

trait DataStoreActorTestBase extends ZenSearchTestBase {

  val dtype = DataType("t")

  val typeEntries = Source.fromInputStream(getClass.getResourceAsStream("/test1.json")).getLines().mkString("\n")
  val data = typeEntries.parseJson match {
    case JsArray(vals) => vals.toList.map(_.asJsObject).zipWithIndex.map{ case (obj, id) => InternalId(dtype, id) -> obj }
    case _ =>
      log.error(s"Cannot parse test data frtom file 'test1.json' - data doesn't read as a Json array")
      Nil
  }
}

class DataStoreActorTest extends DataStoreActorTestBase {

  val indexer = TestActorRef(DataStoreActor.props(dtype, data.toMap))

  test("DataStore Actor should return correct entries for DataLookup message") {

    val id1 = InternalId(dtype, 1)
    val id2 = InternalId(dtype, 2)

    indexer ! DataLookup(id1 :: id2 :: Nil)

    expectMsgPF() {
      case res: DataResult =>
        assert(res.data.size == 2)
        assert(res.data.head.id == id1)
        assert(res.data.head.json == data.collectFirst{ case (id, js) if id == id1 => js }.get)
        assert(res.data(1).id == id2)
        assert(res.data(1).json == data.collectFirst{ case (id, js) if id == id2 => js }.get)
    }
  }
}
