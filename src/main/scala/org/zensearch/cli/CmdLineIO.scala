package org.zensearch.cli

import java.io.{BufferedReader, InputStreamReader, PrintWriter}

trait CmdLineIO {
  def readLine(): String
  def print(line: String): Unit
  def println(line: String): Unit
}

object CmdLineIO {
  case object StdCLIO extends CmdLineIO {
    val stdin = new BufferedReader(new InputStreamReader(System.in))
    val stdout = new PrintWriter(System.out)
    stdout.flush()
    
    def readLine(): String = stdin.readLine
    
    def print(line: String): Unit = {
      stdout.print(line)
      stdout.flush()
    }
    
    def println(line: String): Unit = {
      stdout.println(line)
      stdout.flush()
    }
  }
}
