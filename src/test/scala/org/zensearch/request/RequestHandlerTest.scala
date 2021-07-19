package org.zensearch.request

import akka.actor.{ActorRef, ActorRefFactory, Props}
import akka.testkit.{TestActorRef, TestProbe}
import java.time.LocalDateTime
import spray.json._

import org.zensearch.ZenSearchTestBase
import org.zensearch.main.ConfigModel.{CrossLink, TypeInfo, TypeInfos}
import org.zensearch.messages.Messages.{DataStoreRefs, IndexRefs, LookupRefs}
import org.zensearch.model.Model._
import org.zensearch.util.ChildProvider

trait RequestHandlerTestBase extends ZenSearchTestBase {

  val info1 = TypeInfo(Users.name, "test1.json",
                       CrossLink(Organizations.name, "t1_id", "test2s", "link_id",
                                 "misc" :: Nil, "test1", "t1_id" :: "name" :: Nil, Some(true)) ::
                       CrossLink(Tickets.name, "assoc_t3_id", "assoc_t3", "t3_id",
                                 "t3_id" :: "name" :: Nil, "test1s", "t1_id" :: "name" :: Nil, None) :: Nil)
  val typeInfos = TypeInfos(info1 :: Nil)

  val crit1 = Criteria(Users, "test field 1")
  val crit2 = Criteria(Organizations, "test field 2")
  val indexProxy1 = TestProbe()
  val indexProxy2 = TestProbe()
  val dataProxy1 = TestProbe()
  val dataProxy2 = TestProbe()
  val indexRefs = IndexRefs(Map((crit1 -> indexProxy1.ref), (crit2 -> indexProxy2.ref)))
  val dataRefs = DataStoreRefs(Map((Users -> dataProxy1.ref), (Organizations -> dataProxy2.ref)))
  val lookupRefs = LookupRefs(indexRefs, dataRefs)

  val baseActorProxy = TestProbe()
  val xlinkActorProxy1 = TestProbe()
  val xlinkActorProxy2 = TestProbe()

  val testChildProvider = new ChildProvider {

    var refs2Use: List[ActorRef] = baseActorProxy.ref :: xlinkActorProxy1.ref :: xlinkActorProxy2.ref :: Nil

    var lastProps: List[Props] = Nil

    def newChild(factory: ActorRefFactory, props: Props) = {
      lastProps = props :: lastProps
      var (current, next) = (refs2Use.head, refs2Use.tail)
      refs2Use = next
      current
    }
  }
}

class RequestHandlerTest extends RequestHandlerTestBase {

  val handler = TestActorRef(RequestHandler.props(typeInfos, lookupRefs, testChildProvider))

  val id = 1
  val start = LocalDateTime.now
  val testValue = "Test Value"
  val request = Request(RequestId(id), start, crit1, Some(testValue))

  test("Request Handler should instantiate actor to handle base request") {

    handler ! request

    assert(testChildProvider.lastProps.size == 1)
    assert(testChildProvider.lastProps.head.actorClass() == classOf[RequestActor])

    baseActorProxy.expectMsgPF() {
      case req: Request =>
        assert(req == request)
    }
  }

  val json = """{"t1_id": 21,"name":"name1","assoc_t3_id":33}""".parseJson.asJsObject
  val dataId = InternalId(Users, id)
  val entry = DataEntry(dataId, json)
  val response = Response(request, entry :: Nil)

  test("On receiving a response, Request Handler should determine cross-links and instantiate further actors to handle them") {

    baseActorProxy.reply(response)

    assert(testChildProvider.lastProps.size == 3)
    assert(testChildProvider.lastProps.head.actorClass() == classOf[RequestActor])
    assert(testChildProvider.lastProps(1).actorClass() == classOf[RequestActor])

    xlinkActorProxy1.expectMsgPF() {
      case req: Request =>
        assert(req.criteria.dtype == Organizations)
        assert(req.criteria.field == "link_id")
        assert(req.value == Some("21"))
    }

    xlinkActorProxy2.expectMsgPF() {
      case req: Request =>
        assert(req.criteria.dtype == Tickets)
        assert(req.criteria.field == "t3_id")
        assert(req.value == Some("33"))
    }
  }
}
