package org.zensearch.request

import akka.testkit.{TestActorRef, TestProbe}
import java.time.LocalDateTime
import spray.json.{JsObject, JsString}

import org.zensearch.ZenSearchTestBase
import org.zensearch.messages.Messages._
import org.zensearch.model.Model._

trait RequestActorTestBase extends ZenSearchTestBase {

  val dtype1 = DataType("u")
  val dtype2 = DataType("t")
  val crit1 = Criteria(dtype1, "test field 1")
  val crit2 = Criteria(dtype2, "test field 2")
  val indexProxy1 = TestProbe()
  val indexProxy2 = TestProbe()
  val dataProxy1 = TestProbe()
  val dataProxy2 = TestProbe()
  val indexRefs = IndexRefs(Map((crit1 -> indexProxy1.ref), (crit2 -> indexProxy2.ref)))
  val dataRefs = DataStoreRefs(Map((dtype1 -> dataProxy1.ref), (dtype2 -> dataProxy2.ref)))
  val lookupRefs = LookupRefs(indexRefs, dataRefs)
}

class RequestActorTest extends RequestActorTestBase {

  val reqActor = TestActorRef(RequestActor.props(lookupRefs))

  val id = 1
  val dataId = InternalId(dtype1, id)
  val start = LocalDateTime.now
  val testValue = "Test Value"
  val request = Request(RequestId(id), start, crit1, Some(testValue))

  test("Request Actor should call matching index actor ref for requested lookup") {

    reqActor ! request

    indexProxy1.expectMsgPF() {
      case msg@IndexLookup(Some(value)) =>
        assert(value == testValue)
    }
  }

  test("Request Actor should call matching data-store actor ref for result id from lookup") {

    indexProxy1.reply(IndexResult(dataId :: Nil))

    dataProxy1.expectMsgPF() {
      case msg@DataLookup(ids) =>
        assert(ids.size == 1)
        assert(ids.head == dataId)
    }
  }

  test("Request Actor should pass response back to original caller after data lookup returned") {

    val json = JsObject(Map(("testField" -> JsString("testValue"))))
    val entry = DataEntry(dataId, json)

    dataProxy1.reply(DataResult(entry :: Nil))

    expectMsgPF() {
      case msg@Response(req, endTime, duration, output, xlinks) =>
        assert(req == request)
        assert(endTime.isAfter(request.startTime))
        assert(duration >= 0)
        assert(output.size == 1)
        assert(output.head == entry)
    }
  }
}
