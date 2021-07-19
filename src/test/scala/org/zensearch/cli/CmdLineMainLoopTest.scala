package org.zensearch.cli

import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AnyFunSuite
import org.zensearch.main.SearchHandler
import org.zensearch.messages.Messages.{FormatResult, IndexedFields}
import org.zensearch.model.Model
import org.zensearch.model.Model.{Criteria, DataEntry, FormattedData, InternalId, Organizations, OutputEntry}
import spray.json.JsObject

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, PrintStream}
import System.{lineSeparator => LS}

class CmdLineMainLoopTest extends AnyFunSuite with BeforeAndAfterEach {
  
  import org.zensearch.cli.CmdLineMainLoopTest._
  
  test("Command Line loop should work as expected when given valid inputs with matching results") {
    val commands = Seq("o","name","Acme Inc"," ","q")
    val result1Text = "<formatted output for org 101>"
    val result2Text = "<formatted output for org 103>"
    val expected = Seq(
      "Search on (o)rganizations, (u)sers, or (t)ickets (or ask for (?/h)elp or (q)uit): ",
      "Choose a field to search on - or request a (l)ist of valid fields for organizations: ",
      "Enter the value for that field to search on, or just hit [Return] to search for the field being empty: ",
      s"$LS$LS${LS}Search results:$LS",
      s"${LS}Record found: ${result1Text},",
      s"$LS${LS}Record found: ${result2Text}",
      s"$LS${LS}Records found: 2$LS$LS",
      "Hit [return] to continue",
      "Search on (o)rganizations, (u)sers, or (t)ickets (or ask for (?/h)elp or (q)uit): ")
    val io = new MockConsole(commands)
    val result1 = OutputEntry(
        DataEntry(InternalId(Organizations,101),JsObject()),
         FormattedData(result1Text, Map.empty))
    val result2 = OutputEntry(
        DataEntry(InternalId(Organizations, 103),JsObject()),
         FormattedData(result2Text, Map.empty))
    val result = FormatResult(List(result1, result2))
    val handler = MockSearchHandler(result)
    val fields = IndexedFields(Map((Organizations -> Set(Criteria(Organizations, "name")))))
    new CmdLineMainLoop(io)(handler, fields).run()

    assert(fixLineEnds(io.output.mkString) == fixLineEnds(expected.mkString))
  }
  
  test("Command Line loop should work as expected when given valid inputs with no matching results") {
    val commands = Seq("o","name","Acme Inc"," ","q")
    val expected = Seq(
      "Search on (o)rganizations, (u)sers, or (t)ickets (or ask for (?/h)elp or (q)uit): ",
      "Choose a field to search on - or request a (l)ist of valid fields for organizations: ",
      "Enter the value for that field to search on, or just hit [Return] to search for the field being empty: ",
      s"No matching results found$LS${LS}Hit [return] to continue",
      "Search on (o)rganizations, (u)sers, or (t)ickets (or ask for (?/h)elp or (q)uit): ")
    val io = new MockConsole(commands)
    val result = FormatResult(Nil)
    val handler = MockSearchHandler(result)
    val fields = IndexedFields(Map((Organizations -> Set(Criteria(Organizations, "name")))))
    new CmdLineMainLoop(io)(handler, fields).run()

    assert(fixLineEnds(io.output.mkString) == fixLineEnds(expected.mkString))
  }
  
  test("Display an error when an invalid data type is entered") {
    val commands = Seq("z","q")
    val expected = Seq(
      "Search on (o)rganizations, (u)sers, or (t)ickets (or ask for (?/h)elp or (q)uit): ",
      "input error: 'z' - enter (o)rganizations, (u)sers, (t)ickets, (?/h)elp, or (q)uit",
      "Search on (o)rganizations, (u)sers, or (t)ickets (or ask for (?/h)elp or (q)uit): ")
    val io = new MockConsole(commands)
    val result = FormatResult(Nil)
    val handler = MockSearchHandler(result)
    val fields = IndexedFields(Map((Organizations -> Set(Criteria(Organizations, "_id")))))
    new CmdLineMainLoop(io)(handler, fields).run()
    
    assert(fixLineEnds(io.output.mkString) == fixLineEnds(expected.mkString))
  }
  
  test("Should display contextual help when the 'help' command is entered") {
    val commands = Seq("?","q")
    val expected = Seq(
      "Search on (o)rganizations, (u)sers, or (t)ickets (or ask for (?/h)elp or (q)uit): ",
      "q:                  exit",
      "?:                  display this help",
      "(o)rganizations:    search organizations",
      "(u)sers:            search users",
      "(t)ickets:          search tickets",
      "Search on (o)rganizations, (u)sers, or (t)ickets (or ask for (?/h)elp or (q)uit): ")
    val io = new MockConsole(commands)
    val result = FormatResult(Nil)
    val handler = MockSearchHandler(result)
    val fields = IndexedFields(Map((Organizations -> Set(Criteria(Organizations, "_id")))))
    new CmdLineMainLoop(io)(handler, fields).run()

    assert(fixLineEnds(io.output.mkString) == fixLineEnds(expected.mkString))
  }
  
  test("Command Line loop should connect correctly with std IO") {
  
    val cmds = (s"z${LS}q$LS").getBytes
    val expected =
        """Search on (o)rganizations, (u)sers, or (t)ickets (or ask for (?/h)elp or (q)uit): input error: 'z' - enter (o)rganizations, (u)sers, (t)ickets, (?/h)elp, or (q)uit
          |Search on (o)rganizations, (u)sers, or (t)ickets (or ask for (?/h)elp or (q)uit): """.stripMargin
          
    System.setIn(new ByteArrayInputStream(cmds))
    val out = new ByteArrayOutputStream
    System.setOut(new PrintStream(out))
    
    val fields = IndexedFields(Map((Organizations -> Set(Criteria(Organizations, "_id")))))
    StdCmdLineLoop(MockSearchHandler(FormatResult(Nil)), fields).run()
    
    assert(fixLineEnds(out.toString) == fixLineEnds(expected))
  }
  
}

object CmdLineMainLoopTest {
  class MockConsole(commands: Seq[String]) extends CmdLineIO {
    var output: Seq[String] = Seq()
    var input: Seq[String] = commands
    
    override def readLine(): String = {
      val result = input.head
      input = input.drop(1)
      result
    }
  
    override def print(str: String): Unit = {
        output = output :+ str
      }
  
    override def println(line: String): Unit = this.print(s"$line")
  }
  
  case class MockSearchHandler(result: FormatResult) extends SearchHandler {
    override def send(request: Model.Request) = result
    override def shutdown: Unit = ()
  }
  
  private def fixLineEnds(str: String) =
      str.replaceAll("\r\n","<CR>")
         .replaceAll("\r","<CR>")
         .replaceAll("\n","<CR>")
         .replaceAll(LS,"<CR>")
}
