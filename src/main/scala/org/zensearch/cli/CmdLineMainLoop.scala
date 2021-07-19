package org.zensearch.cli

import scala.annotation.tailrec
import System.{lineSeparator => LS}
import org.zensearch.main.SearchHandler
import org.zensearch.messages.Messages.{FormatResult, IndexedFields}
import org.zensearch.model.Model.{Criteria, DataType, Organizations, Request, RequestId, Tickets, Users}

trait State {
  def action(): State
}

object Done extends State {
  def action() = this
}

trait InputState extends State {
  
  val QUIT = "q"
  val QUIT_MSG = "(q)uit"
  val HELP = "?"
  val HELP2 = "h"
  val HELP_MSG = "(?/h)elp"
  
  def commandHelp: Seq[(String,String)] = Seq(
    (QUIT, "exit"),
    (HELP, "display this help")
  )
  
  def console: CmdLineIO
  def prompt: String
  def next(input: String): State
  
  def action() = {
    console.print(prompt)
    console.readLine().trim match { //.toLowerCase
      case cmd if cmd == QUIT => Done
      case cmd if cmd == HELP || cmd == HELP2 => DisplayHelp(this)
      case cmd => next(cmd)
    }
  }
  
  def onError(input: String) = console.println(s"input error: $input")
}

case class DisplayHelp(currState: InputState) extends State {
  def action() = {
    currState.commandHelp foreach { case (command, purpose) =>
      currState.console.println(f"${command+":"}%-20s$purpose")
    }
    currState.action()
  }
}

trait LoopStates {
  
  implicit val handler: SearchHandler
  implicit val fields: IndexedFields
  
  case class QueryType(val console: CmdLineIO)(implicit handler: SearchHandler) extends InputState {
    
    val selectOrg = "(o)rganizations"
    val selectUser = "(u)sers"
    val selectTicket = "(t)ickets"
    
    val prompt = s"Search on $selectOrg, $selectUser, or $selectTicket (or ask for (?/h)elp or $QUIT_MSG): "
    
    def next(queryOn: String): State = {
      queryOn match {
        case qon if Organizations.name.startsWith(qon) => QueryField(console, Organizations)
        case qon if Users.name.startsWith(qon) => QueryField(console, Users)
        case qon if Tickets.name.startsWith(qon) => QueryField(console, Tickets)
        case error =>
          onError(s"'$error' - enter $selectOrg, $selectUser, $selectTicket, $HELP_MSG, or $QUIT_MSG")
          this
      }
    }
    
    override val commandHelp = super.commandHelp ++ Seq(
        (selectOrg, "search organizations"),
        (selectUser, "search users"),
        (selectTicket, "search tickets")
      )
  }
  
  case class QueryField(console: CmdLineIO, dtype: DataType)(implicit fields: IndexedFields) extends InputState {

    val LIST = "list"
    val searchable: List[String] = fields.fields.get(dtype).map(_.map(_.field)).get.toList
    val prompt = s"Choose a field to search on - or request a (l)ist of valid fields for ${dtype.name}s: "

    def next(queryField: String): State = queryField match {
      case list if LIST.startsWith(list) =>
        console.println(s"Searchable fields for ${dtype.name}s:")
        console.println(searchable.mkString(LS))
        this
      case field if !searchable.filter(_.startsWith(queryField)).isEmpty => QueryValue(console, Criteria(dtype, field))
      case _ =>
        onError(s"not a recognised ${dtype.name} field: '$queryField'")
        this
    }
    
    override val commandHelp: Seq[(String,String)] = super.commandHelp ++ Seq((LIST, s"see a list of valid fields for ${dtype.name}s"))
  }
  
  case class QueryValue(console: CmdLineIO, crit: Criteria) extends InputState {
    
    val prompt = "Enter the value for that field to search on, or just hit [Return] to search for the field being empty: "
    
    def next(value: String): State = ShowResult(console, handler.send(Request(crit, Option(value.trim).filter(!_.isEmpty))))
  }
  
  case class ShowResult(console: CmdLineIO, result: FormatResult)(implicit handler: SearchHandler) extends InputState {
    val recSep = s",$LS${LS}Record found: "
    val prompt = if (result.data.isEmpty) {
          s"No matching results found$LS${LS}Hit [return] to continue"
        } else {
          s"$LS$LS${LS}Search results:$LS$LS" +
          result.data.map(_.text.text.trim)
                .mkString("Record found: ", recSep, s"$LS${LS}Records found: ${result.data.size}$LS$LS") +
          "Hit [return] to continue"
        }
    
    def next(value: String): State = QueryType(console)
  }
}

class CmdLineMainLoop(console: CmdLineIO)(implicit val handler: SearchHandler, val fields: IndexedFields) extends LoopStates {
  
  @tailrec
  private def inputLoop(next: State): State = next match {
    case Done =>
      handler.shutdown
      Done
    case curr => inputLoop(curr.action())
  }
  
  def run() = {
    inputLoop(QueryType(console))
  }
}

case class StdCmdLineLoop(searchHandler: SearchHandler, indexedFields: IndexedFields)
    extends CmdLineMainLoop(CmdLineIO.StdCLIO)(searchHandler, indexedFields)
