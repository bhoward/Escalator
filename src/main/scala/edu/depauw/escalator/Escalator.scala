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

import java.io.{File, StringWriter, PrintWriter}

import scala.tools.nsc.{Interpreter => ScalaInterpreter, Settings,
      InterpreterResults => IR}

import org.github.scopt._

object Escalator {
  val config = new Config
  
  val writer = new StringWriter()
  
  var interpreter: ScalaInterpreter = _ // assigned in init()
  
  var specCount = 1

  val server = new Thread {
    override def run() = {
      val resourceHome = "/site"
      new EscHTTPD(config.port, resourceHome, config.target)
    }
  }
  server.setDaemon(true) // so JVM will shut down when main thread exits
  var serverStarted = false
  
  /**
   * Initialize the Escalator object from the command line parameters. This must be called first.
   */
  def init(args: Array[String], gui: Option[GUI]) {
    handleArgs(args)
    updateClasspath()
    config.gui = gui
  }
  
  /**
   * Call this after setting the classpath in config
   */
  private def updateClasspath() {
    val settings = {
      System.setProperty("scala.usejavacp", "true") // makes the 2.8 interpreter use java classpath
      new Settings()
    }
    
    settings.classpath.value = settings.classpath.value + File.pathSeparator + config.classpath
    interpreter = new ScalaInterpreter(settings, new PrintWriter(writer))
  }
  
  private def handleArgs(args: Array[String]) {
    var rest: List[String] = Nil
      
    val parser = new OptionParser("escalator") {
      opt("cp", "classpath", "<path>", "classpath for running examples and tests", {
        v: String => config.classpath = v
      })
      intOpt("p", "port", "port for localhost httpd (default: 8000)", {
        v: Int => config.port = v
      })
      arglist("[source [target]]", "input and output directories (default: . and esc-site)", {
        v: String => rest = rest ::: List(v)
      })
    }
    
    // Only call the parser if args is non-empty
    // TODO OptionParser should allow empty arglist...
    if (args.length > 0 && !parser.parse(args)) {
      // Something was wrong
      System.exit(1)
    }
    
    val source = if (rest.length > 0) {
      new File(rest(0))
    } else {
      new File(System.getProperty("user.dir"))
    }
    
    val target = if (rest.length > 1) {
      new File(rest(1))
    } else {
      new File(source, "esc-site")
    }
    
    if (rest.length > 2) {
      Console.err.println("Too many arguments")
      exit(1)
    }
    
    config.source = source
    config.target = target
  }
  
  def setSource(source: File) {
    config.source = source
    if (config.classpath == ".") {
      // This is a hack, but it handles the simplest case of no arguments
      config.classpath = source.getAbsolutePath
    }
  }
  
  def getSource: File = config.source
  
  def setTarget(target: File) {
    config.target = target
  }
  
  def getTarget: File = config.target
  
  def setClasspath(classpath: String) {
    config.classpath = classpath
    updateClasspath()
  }
  
  def getClasspath: String = config.classpath
  
  def process() {
    // Check the directories
    if (!config.source.exists || !config.source.isDirectory) {
      Console.err.println("Source directory invalid")
      exit(1)
    }
    
    if (!config.target.exists) {
      config.target.mkdirs
    }
    
    if (!config.target.exists || !config.target.isDirectory) {
      Console.err.println("Target directory invalid")
    }
    
    // Build the source tree
    val root = TreeNode(config.source)
    
    // Create the output
    root.generate(config.target, None, config)
    
    // Start the server if not already running, unless port is zero (headless mode)
    if (config.port != 0) {
      if (!serverStarted) {
        server.start()
        serverStarted = true
      }
      
      // Open a browser window
      com.centerkey.utils.BareBonesBrowserLaunch.openURL("http://localhost:" +
          config.port + "/project/")
    
      if (config.gui.isEmpty) {
        println("Ready - Press Return to Quit")
        System.in.read()
      }
    }
  }
  
  def runExample(code: String): String = {
    var expr = ""; var ret = "> "
    val escout = new EscOutputStream
    
    scala.Console.withOut(escout) {
      for (line <- code.lines) {
        expr += line + "\n"
        
        if (expr.trim == "") {
          // Skip this line
        } else if (interpreter.interpret(expr) == IR.Incomplete) {
          ret += line + "\n| "
        } else {
          ret += line + "\n" + escout.string + writer.getBuffer + "\n> "
          escout.clear()
          writer.getBuffer.delete(0, writer.getBuffer.length)
          expr = ""
        }
      }
    }
      
    ret.dropRight(2) // Remove trailing prompt
  }
  
  def runTest(code: String): String = {
    val escout = new EscOutputStream
    
    scala.Console.withOut(escout) {
      val testName = "Spec" + specCount
      specCount += 1
    
      interpreter.interpret("class "+testName+
        " extends org.specs.Specification with org.specs.ScalaCheck {\n" +
        "import org.specs._\n" +
        "import org.scalacheck._\n" +
        code +
        "}\n" + "(new "+testName+").reportSpecs")

      writer.getBuffer.delete(0, writer.getBuffer.length)
    }
    
    escout.string
  }
}