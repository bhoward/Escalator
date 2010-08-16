// Copyright 2010, Brian T. Howard (bhoward@depauw.edu)
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package edu.depauw.escalator

import java.io._
import scala.tools.nsc.{Interpreter => ScalaInterpreter, Settings,
			InterpreterResults => IR}

object Downalate {
  var id = 0
  val ESCESC = "//"
  
  val ExampleStart = (ESCESC + """\s*example\s*""").r
  val ExampleEnd = (ESCESC + """\s*end\s*example\s*""").r
  val TestStart = (ESCESC + """\s*test\s*""").r
  val TestEnd = (ESCESC + """\s*end\s*test\s*""").r
  val ConsoleExampleStart = (ESCESC + """\s*console\s*example\s*""").r
  val ConsoleTestStart = (ESCESC + """\s*console\s*test\s*""").r
  val SourceCmd = (ESCESC + """\s*source\s*(.*)""").r
  val FilePart = """('(?:\\'|[^'])*'|\S*)\s*(.*?)\s*""".r
  
  class DefaultState(val processed: String) {
    def add(line: String) = line match {
      case ExampleStart() =>
        new ExampleState(processed, "")
      case TestStart() =>
        new TestState(processed, "")
      case ConsoleExampleStart() =>
        new ConsoleExampleState(processed, "")
      case ConsoleTestStart() =>
        new ConsoleTestState(processed, "")
      case SourceCmd(rest) =>
        new DefaultState(join(processed, wrap(getSource(rest))))
      case _ =>
        new DefaultState(join(processed, line))
    }
    
    def wrap(p: (String, Seq[(String, Int)])) = {
      val (path, chunks) = p
      val (_, ext) = Util.baseExtension(path)
      
      "\n" +
      <div><small><a href={ path + ".html" }>{ path }</a></small>{
        for ((code, line) <- chunks) yield
          <pre class={"brush: " + ext + "; first-line: " + line + ";"}>{ code }</pre>
      }</div> + "\n"
    }
    
    def wrapResults(code: String, spec: Boolean) = {
      id += 1
      val rawId = "raw" + id
      val runId = "run" + id
      val showRaw = "document.getElementById('" + rawId +
        "').style.display='block';document.getElementById('" + runId +
        "').style.display='none';return false;"
      val showRun = "document.getElementById('" + rawId +
        "').style.display='none';document.getElementById('" + runId +
        "').style.display='block';return false;"
        
      <div id={rawId} style="display: none;">
        <small><a href="#" onclick={ showRun }>Show result</a></small>
        <pre class="brush: scala;">{ code }</pre>
      </div> + "\n" +
      <div id={runId} style="display: block;">
        <small><a href="#" onclick={ showRaw }>Show source</a></small>
        {
          if (spec)
            <pre class="brush: specs; light: true;">{ Escalator.runTest(code) }</pre>
          else 
            <pre class="brush: plain; light: true;">{ Escalator.runExample(code) }</pre>
        }
      </div> + "\n"
    }
    
    def wrapConsoleResults(code: String, spec: Boolean) = {
      id += 1
      val srcId = "src" + id
      val resId = "res" + id
      val rows = math.max(6, code.split("\n").size)
      
      <div>
        <textarea id={srcId} cols="80" rows={rows.toString}
          onkeydown={"return Escalator.handleKey(event, " + id + ", " + spec + ")"}>{ code }</textarea>
        <small><a href="#" onclick={"Escalator.getResults(" + id + ", " + spec + ")"}>Run</a></small>
      </div> + "\n" +
      <div id={resId} style="background-color: #e0e0e0;">Click 'Run' or press Ctrl-Enter for results</div>
    }
      
    def join(a: String, b: String) = a + "\n" + b
    
    /**
     * Joins just the strings returned from a call to getSource.
     */
    def extract(p: (String, Seq[(String, Int)])) = {
      (for ((s, _) <- p._2) yield s).mkString("\n")
    }
  }
  
  class TestState(processed: String, buffer: String) extends DefaultState(processed) {
    override def add(line: String) = line match {
      case TestEnd() =>
        new DefaultState(join(processed, wrapResults(buffer, true)))
      case SourceCmd(rest) =>
        new TestState(processed, join(buffer, extract(getSource(rest))))
      case _ =>
        new TestState(processed, join(buffer, line))
    }
  }
  
  class ExampleState(processed: String, buffer: String) extends DefaultState(processed) {
    override def add(line: String) = line match {
      case ExampleEnd() =>
        new DefaultState(join(processed, wrapResults(buffer, false)))
      case SourceCmd(rest) =>
        new ExampleState(processed, join(buffer, extract(getSource(rest))))
      case _ =>
        new ExampleState(processed, join(buffer, line))
    }
  }
  
  class ConsoleTestState(processed: String, buffer: String) extends DefaultState(processed) {
    override def add(line: String) = line match {
      case TestEnd() =>
        new DefaultState(join(processed, wrapConsoleResults(buffer, true)))
      case SourceCmd(rest) =>
        new ConsoleTestState(processed, join(buffer, extract(getSource(rest))))
      case _ =>
        new ConsoleTestState(processed, join(buffer, line))
    }
  }
  
  class ConsoleExampleState(processed: String, buffer: String) extends DefaultState(processed) {
    override def add(line: String) = line match {
      case ExampleEnd() =>
        new DefaultState(join(processed, wrapConsoleResults(buffer, false)))
      case SourceCmd(rest) =>
        new ConsoleExampleState(processed, join(buffer, extract(getSource(rest))))
      case _ =>
        new ConsoleExampleState(processed, join(buffer, line))
    }
  }
  
  /**
   * Expand all of the escalator commands in the source.
   * 
   * @param source Incoming escalator document, as a String
   * @return the expanded markdown version of the document
   */
  def apply(source: String): String = {
    Escalator.interpreter.reset()
    source.lines.foldLeft(new DefaultState(""))(_ add _).processed
  }
  
  /**
   * Retrieve a source fragment from a file.  The command consists of a filename
   * (in single quotes in case it contains spaces), optionally followed by either
   * a label in angle brackets or a sequence of pattern selectors.
   * 
   * @param command The tail part of the //source ... command.
   * @return a pair of the filename and a sequence of pairs of source fragments and
   * their starting line numbers from the file
   */
  def getSource(command: String): (String, Seq[(String, Int)]) = {
    command match {
      case FilePart(path, rest) => {
        val path2 = Util.unquote(path)
        val file = if (path2 startsWith "/") {
          new File(Escalator.config.source, path2)
        } else {
          new File(Escalator.config.documentPath.getParentFile, path2)
        }
        
        if (rest == "") {
          // Slurp in the whole file
          (path2, List((Util.readFile(file), 1)))
          
        } else if ((rest startsWith "<") && (rest endsWith ">")) {
          // Extract a labeled fragment
          val label = rest.substring(1, rest.length - 1)
          (path2, List(getLabeledSource(file, label)))
          
        } else {
          // Find a fragment given a selector path
          (path2, Find(file, rest))
        }
      }
      
      case _ => ("Error in source command", Nil)
    }
  }
  
  /**
   * This works to grab source code out of another file, which should be
   * denoted in the escalator file being read.  
   * 
   * @return Returns the source code if found, otherwise returns a message
   * saying it couldn't find the named segment requested at the path provided.
   */
  def getLabeledSource(file: File, label: String): (String, Int) = {
    val source = Util.readFile(file)
    val matchOpt = ("(?s)(.*?)([ \\t]*" + ESCESC + "<"+label+">\\s*)" + "(.*?)" +
                    "\\s*?" + ESCESC + "</"+label+">").r.findFirstMatchIn( source )
                    
    matchOpt match {
      case Some(m) => (m.group(3), m.group(1).lines.length + 2)
      
      case None => ("[!<"+label+"> not found in "+file+"!]", 0)
    }
  }
}
