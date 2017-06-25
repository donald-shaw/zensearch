package org.zensearch.main

import akka.actor.{ActorRef, ActorRefFactory, Props}
import akka.testkit.{TestActorRef, TestProbe}

import scala.concurrent.duration._
import org.zensearch.ZenSearchTestBase
import org.zensearch.formatting.FormattingActor
import org.zensearch.main.ConfigModel.{TypeInfo, TypeInfos}
import org.zensearch.messages.Messages.{DataStoreRefs, IndexRefs, LookupRefs}
import org.zensearch.model.Model._
import org.zensearch.parsing.ParserManager
import org.zensearch.parsing.ParserMessages.GetLookupRefs
import org.zensearch.request.RequestHandler
import org.zensearch.util.ChildProvider


trait RequestManagerTestBase extends ZenSearchTestBase {

  val dtype1 = DataType("test type 1")

  val req = Request(Criteria(dtype1, "name"), None)

  val lookupRefs = LookupRefs(IndexRefs(Map.empty), DataStoreRefs(Map.empty))

  val cfgMgrProxy = TestProbe()
  val parserMgrProxy = TestProbe()
  val formatterProxy = TestProbe()
  val handlerProxy = TestProbe()

  val testChildProvider = new ChildProvider {

    var refs2Use: List[ActorRef] = cfgMgrProxy.ref :: parserMgrProxy.ref :: formatterProxy.ref :: handlerProxy.ref :: Nil

    var lastProps: List[Props] = Nil

    def newChild(factory: ActorRefFactory, props: Props) = {
      lastProps = props :: lastProps
      var (current, next) = (refs2Use.head, refs2Use.tail)
      refs2Use = next
      current
    }
  }
}

class RequestManagerTest extends RequestManagerTestBase {

  var reqMgr: TestActorRef[_] = null

  test("Request Manager should spawn a config manager and parser manager") {

    reqMgr = TestActorRef(RequestManager.props(testChildProvider))

    assert(testChildProvider.lastProps.size == 2)
    assert(testChildProvider.lastProps.head.actorClass() == classOf[ParserManager])
    assert(testChildProvider.lastProps.last.actorClass() == classOf[ConfigManager])
  }

  // Note: follows on from previous test
  test("Request Manager should stash requests while initiallising") {

    reqMgr ! req

    expectNoMsg(100 milliseconds)

    val typeInfo = TypeInfo("type1", "type1file", Nil)
    val typeInfos = TypeInfos(typeInfo :: Nil)

    reqMgr ! typeInfos

    parserMgrProxy.expectMsgPF() {
      case GetLookupRefs(tInfo) =>
        assert(tInfo == typeInfos)
    }
  }

  // Note: follows on from previous test
  test("Once fully initiallised, Request Manager should unstash request and spawn a request actor to handle it") {

     parserMgrProxy.reply(lookupRefs)

     assert(testChildProvider.lastProps.size == 4)
     assert(testChildProvider.lastProps.head.actorClass() == classOf[RequestHandler])
     assert(testChildProvider.lastProps(1).actorClass() == classOf[FormattingActor])

     handlerProxy.expectMsgPF() {
       case msg: Request =>
         assert(msg == req)
     }
  }

  // Note: follows on from previous test
  test("Request Manager should pass responses to formatter to format") {

     val resp = Response(req, Nil)

     handlerProxy.reply(resp)

     formatterProxy.expectMsgPF() {
       case msg: Response =>
         assert(msg == resp)
     }
  }
}
