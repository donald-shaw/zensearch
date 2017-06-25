package org.zensearch.formatting

import akka.testkit.{TestActorRef, TestProbe}
import spray.json._

import org.zensearch.ZenSearchTestBase
import org.zensearch.messages.Messages.{DataStoreRefs, FormatResult}
import org.zensearch.model.Model._

trait FormattingActorTestBase extends ZenSearchTestBase {

  val dtype1 = DataType("test1")

  val dsProxy1 = TestProbe()
  val reqMgrProxy = TestProbe()

  val dataStoreRefs = DataStoreRefs(Map((dtype1 -> dsProxy1.ref)))
}

class FormattingActorTest extends FormattingActorTestBase {

  val formatter = TestActorRef(FormattingActor.props(dataStoreRefs, reqMgrProxy.ref))

  test("DataStore Actor should return correct entries for DataLookup message") {

    val id1 = InternalId(dtype1, 1)
    val id2 = InternalId(dtype1, 2)

    val json1 = """{"_id": 21,"name":"name1"}""".parseJson.asJsObject
    val json2 = """{"_id": 42,"name":"name2"}""".parseJson.asJsObject

    val expected1 = json1.prettyPrint
    val expected2 = json2.prettyPrint

    val req = Request(Criteria(dtype1, "name"), None)
    val data1 = DataEntry(id1, json1)
    val data2 = DataEntry(id2, json2)
    val resp = Response(req, data1 :: data2 :: Nil)

    formatter ! resp

    expectMsgPF() {
      case res: FormatResult =>
        assert(res.data.size == 2)
        assert(res.data.head.data == data1)
        assert(res.data.head.text.text == expected1)
        assert(res.data(1).data == data2)
        assert(res.data(1).text.text == expected2)
    }
  }
}
