package org.zensearch.main

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try
import org.zensearch.cli.StdCmdLineLoop
import org.zensearch.formatting.{DefaultFormatterFactory, FormatterFactory, TextFormatterFactory}
import org.zensearch.messages.Messages.{FormatResult, IndexedFields, RequestIndexedFields}
import org.zensearch.model.Model.Request
import org.zensearch.util.ChildProvider

trait SearchHandler {
  def send(request: Request): FormatResult
  def shutdown: Unit
}

object ZenSearchMain extends App with SearchHandler {

  private val appName = "ZenSearch"
  private val queryTimeout = 4 seconds
  implicit lazy val timeout: akka.util.Timeout = queryTimeout
  private val defaultSystemMaxUptime: Duration = 5 minutes // Safest to have a not-to-long maximum for a code test set-up
  
  implicit lazy val system = ActorSystem(appName)
  lazy val log = system.log

  private var requestMgr: ActorRef = null
  
  run(args)
  
  def run(args: Array[String]): Unit = {
    val (sysMaxUptime, formatterFactory) = readArgs(args)
    startApp(sysMaxUptime, formatterFactory)
  }

  def send(request: Request) = Await.result((requestMgr ? request).mapTo[FormatResult], queryTimeout)

  def shutdown = system.terminate()

  private def startApp(sysMaxUptime: Duration, formatterFactory: FormatterFactory): Unit = {
    log.warning(s"\n\n\t *** Starting ${appName} *** \n\n\n")
    val fields = startRequestManager(formatterFactory)
    val loop = StdCmdLineLoop(this, fields)
    loop.run()
    Await.ready(system.whenTerminated, sysMaxUptime)
    log.warning(s"\n\nFinished - exiting ${appName}\n")
  }

  private def startRequestManager(formatterFactory: FormatterFactory): IndexedFields = {
    requestMgr = system.actorOf(RequestManager.props(ChildProvider.default, formatterFactory))
    Await.result((requestMgr ? RequestIndexedFields).mapTo[IndexedFields], queryTimeout)
  }
  
  private def readArgs(args: Array[String]) = {
    var duration = defaultSystemMaxUptime
    var formatterFactory: FormatterFactory = DefaultFormatterFactory
    var skipNextArg: Boolean = false
    for (arg_w_ix <- args.zipWithIndex) {
      (arg_w_ix, skipNextArg) match {
        case (_, true) => skipNextArg = false
        case ((opt, ix), _) if opt == "-d" || opt == "--duration" =>
          duration = Try(args(ix+1).toInt).map(_ seconds).getOrElse(duration)
          skipNextArg = true
        case ((opt, ix), _) if opt == "-f" || opt == "--formatter" =>
          if (args(ix+1) == "text") formatterFactory = TextFormatterFactory
          skipNextArg = true
        case _ => // unrecognised arg - do nothing
      }
    }
    (duration, formatterFactory)
  }
}
