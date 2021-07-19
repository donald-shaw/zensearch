package org.zensearch.main

import akka.actor.{ActorRef, ActorRefFactory, Props}
import akka.testkit.{TestActorRef, TestProbe}
import scala.concurrent.duration._

import org.zensearch.ZenSearchTestBase
import org.zensearch.formatting.{Formatter, FormatterFactory}
import org.zensearch.main.ConfigModel.{TypeInfo, TypeInfos}
import org.zensearch.messages.Messages.{DataStoreRefs, FormatResult, IndexRefs, LookupRefs}
import org.zensearch.model.Model._
import org.zensearch.parsing.ParserManager
import org.zensearch.parsing.ParserMessages.GetLookupRefs
import org.zensearch.request.RequestHandler
import org.zensearch.util.ChildProvider


trait RequestManagerTestBase extends ZenSearchTestBase {

  val dtype1 = DataType("t")

  val req = Request(Criteria(dtype1, "name"), None)

  val lookupRefs = LookupRefs(IndexRefs(Map.empty), DataStoreRefs(Map.empty))

  val cfgMgrProxy = TestProbe()
  val parserMgrProxy = TestProbe()
  val handlerProxy = TestProbe()

  val testChildProvider = new ChildProvider {

    var refs2Use: List[ActorRef] = cfgMgrProxy.ref :: parserMgrProxy.ref :: handlerProxy.ref :: Nil

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
  
  val resp = Response(req, Nil)
  
  val formatterFactory = new FormatterFactory {
  
    val expected = FormatResult(Nil)
  
    override def apply() = new Formatter {
      override def formatResponse(baseResp: Response) =
        if (baseResp == resp) expected else null // Only return expected formatted output if receive expected input
    }
  }
  
  test("Request Manager should spawn a config manager and parser manager") {

    reqMgr = TestActorRef(RequestManager.props(testChildProvider, formatterFactory))

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

     assert(testChildProvider.lastProps.size == 3)
     assert(testChildProvider.lastProps.head.actorClass() == classOf[RequestHandler])

     handlerProxy.expectMsgPF() {
       case msg: Request =>
         assert(msg == req)
     }
  }
  
  // Note: follows on from previous test
  test("Request Manager should return expected formatted response") {

     handlerProxy.reply(resp)
     
    expectMsgPF() {
      case res: FormatResult =>
        assert(res == formatterFactory.expected)
    }
  }
}
