package org.zensearch.data

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKitBase}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import scala.concurrent.duration._

import org.zensearch.main.RequestManager
import org.zensearch.messages.Messages.FormatResult
import org.zensearch.model.Model.{Criteria, DataType, Request}
import org.zensearch.util.ChildProvider

class ZenSearchIntegrationTestBase extends AnyFunSuite with TestKitBase with ImplicitSender with BeforeAndAfterAll {

  implicit lazy val system = ActorSystem("ITest")
  lazy val log = system.log
  lazy val queryTimeout = 4 seconds
  implicit lazy val timeout: akka.util.Timeout = queryTimeout

  override protected def afterAll(): Unit = {
    system.terminate()
    super.afterAll()
  }
}

class ZenSearchIntegrationTest extends ZenSearchIntegrationTestBase {

  test("Integration test - retrieve user with specific name") {

    val requestMgr = system.actorOf(RequestManager.props(ChildProvider.default))

    val userType = DataType("user")
    val crit = Criteria(userType, "name")
    val value = Some("Prince Hinton")
    val req = Request(crit, value)

    requestMgr ! req

    expectMsgPF() {
      case FormatResult(entries) =>
        assert(entries.size == 1)
        assert(entries.head.text.text.contains("Prince Hinton"))
        assert(entries.head.text.text.contains(""""_id": 71"""))
        assert(entries.head.text.text.contains("danahinton@flotonic.com"))
        assert(entries.head.text.text.contains(""""tickets": ["""))
        assert(entries.head.text.text.contains(""""A Catastrophe in Micronesia"],"""))
    }
  }

  test("Integration test - retrieve tickets with specific submitter id") {

    val requestMgr = system.actorOf(RequestManager.props(ChildProvider.default))

    val userType = DataType("ticket")
    val crit = Criteria(userType, "submitter_id")
    val value = Some("71")
    val req = Request(crit, value)

    requestMgr ! req

    expectMsgPF() {
      case FormatResult(entries) =>
        assert(entries.size == 3)
        assert(entries.head.text.text.contains("A Drama in Wallis and Futuna Islands"))
        assert(entries.head.text.text.contains(""""submitter": {"""))
        assert(entries.head.text.text.contains(""""name": "Prince Hinton""""))
    }
  }

  test("Integration test - retrieve organizations with specific 'details' value") {

    val requestMgr = system.actorOf(RequestManager.props(ChildProvider.default))

    val userType = DataType("organization")
    val crit = Criteria(userType, "details")
    val value = Some("Non profit")
    val req = Request(crit, value)

    requestMgr ! req

    expectMsgPF() {
      case FormatResult(entries) =>
        assert(entries.size == 7)
        assert(entries.head.text.text.contains("Non profit"))
        assert(entries.head.text.text.contains(""""users": [{"""))
        assert(entries.head.text.text.contains(""""name": "Finley Conrad""""))
        assert(entries.head.text.text.contains(""""_id": 46"""))
    }
  }
}
