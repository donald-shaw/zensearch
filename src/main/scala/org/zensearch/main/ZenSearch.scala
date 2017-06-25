package org.zensearch.main

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import scala.concurrent.Await
import scala.concurrent.duration._

import org.zensearch.messages.Messages.FormatResult
import org.zensearch.model.Model.Request
import org.zensearch.util.ChildProvider

object ZenSearchMain extends App {

  lazy val systemName = "ZenSearch"
  lazy val queryTimeout = 4 seconds
  implicit lazy val timeout: akka.util.Timeout = queryTimeout
  def defaultSystemMaxUptime: Duration = 10 minutes // @TODO - DSS: safer for now
  def readMaxUptime(args: Array[String]) = defaultSystemMaxUptime // Just always use default for now
  implicit lazy val system = ActorSystem(systemName)
  lazy val log = system.log

  private var requestMgr: ActorRef = null

  def run(args: Array[String]): Unit = startApp(readMaxUptime(args))

  def startApp(sysMaxUptime: Duration): Unit = {
    startRequestManager()
    Await.ready(system.whenTerminated, sysMaxUptime)
  }

  def startRequestManager(): Unit = {
    requestMgr = system.actorOf(RequestManager.props(ChildProvider.default))
  }

  def send(request: Request) = Await.result((requestMgr ? request).mapTo[FormatResult], queryTimeout)

  def shutdown = system.terminate()
}

object ZenSearchCli {

  import ZenSearchMain._
  import org.zensearch.model.Model._

  def start = {
    val cli = ZenSearchCli(system, queryTimeout) // Forces instantiation of actor system.
    startRequestManager()
    log.info(s"\n\nWelcome to Zen-Search\n\n")
  }

  def exit = {
    shutdown
    sys.exit
  }

  case object InitRequest {
    def users = DataType("user")
    def tickets = DataType("ticket")
    def orgs = DataType("organization")
  }
  def find = InitRequest

  def users = DataType("user")
  def tickets = DataType("ticket")
  def organizations = DataType("organization")

  case class CritBuilder(dtype: DataType) {
    def where(field: String) = Criteria(dtype, field)
  }
  implicit def type2criteria(dtype: DataType): CritBuilder = CritBuilder(dtype)

  case class RequestBuilder(crit: Criteria) {
    def is(value: String): Unit = {
      val result = send(Request(crit, Some(value)))
      log.info("\n\n\nSearch results:\n\n"+
        result.data.map(_.text.text.trim)
              .mkString("Record found: ", ",\n\nRecord found: ",s"\n\nRecords found: ${result.data.size}"))
    }
    def is(value: Int): Unit = is(value.toString)
  }
  implicit def crit2Req(crit: Criteria): RequestBuilder = RequestBuilder(crit)
}

case class ZenSearchCli(system: ActorSystem, queryTimeout: FiniteDuration)
