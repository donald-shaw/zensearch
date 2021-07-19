package org.zensearch.indexing

import akka.testkit.TestActorRef
import scala.io.Source
import spray.json._

import org.zensearch.ZenSearchTestBase
import org.zensearch.messages.Messages.{IndexLookup, IndexResult}
import org.zensearch.model.Model.{Criteria, DataType, InternalId}

trait IndexActorTestBase extends ZenSearchTestBase {

  val dtype1 = DataType("t")
  val crit1 = Criteria(dtype1, "misc")

  val typeEntries = Source.fromInputStream(getClass.getResourceAsStream("/test1.json")).getLines().mkString("\n")
  val data = typeEntries.parseJson match {
    case JsArray(vals) => vals.toList.map(_.asJsObject)
    case _ =>
      log.error(s"Cannot parse test data frtom file 'test1.json' - data doesn't read as a Json array")
      Nil
  }
}

class IndexActorTest extends IndexActorTestBase {

  val indexer = TestActorRef(IndexActor.props(crit1, data))

  test("Index Actor should return correct single result for IndexLookup message with value") {

    val value = "t1 misc 1"

    indexer ! IndexLookup(Some(value))

    expectMsgPF() {
      case res: IndexResult =>
        assert(res.ids.size == 1)
        assert(res.ids.head == InternalId(dtype1, 0))
    }
  }

  test("Index Actor should return correct single result for IndexLookup message with no value") {

    indexer ! IndexLookup(None)

    expectMsgPF() {
      case res: IndexResult =>
        assert(res.ids.size == 1)
        assert(res.ids.head == InternalId(dtype1, 2))
    }
  }

  test("Index Actor should return correct multiple results for IndexLookup message with value matching multiple entries") {

    val value = "t1 misc 2"

    indexer ! IndexLookup(Some(value))

    expectMsgPF() {
    case res: IndexResult =>
      assert(res.ids.size == 2)
      assert(res.ids.head == InternalId(dtype1, 1))
      assert(res.ids(1) == InternalId(dtype1, 3))
    }
  }
  
  test("Index Actor should return correct results for IndexLookup message targeting array field") {
    
    val crit2 = Criteria(dtype1, "tags")
    val tagIndexer = TestActorRef(IndexActor.props(crit2, data))
    tagIndexer ! IndexLookup(Some("Tag3"))

    expectMsgPF() {
      case res: IndexResult =>
        assert(res.ids.size == 2)
        assert(res.ids.head == InternalId(dtype1, 0))
        assert(res.ids(1) == InternalId(dtype1, 2))
    }
  }
}
