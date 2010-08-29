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

import java.io.{File, StringWriter, PrintWriter, FileInputStream}

import scala.tools.nsc.{Interpreter => ScalaInterpreter, Settings,
      InterpreterResults => IR}

object Escalator {
  val config = new Config
  var showGUI: Boolean = false
  var gui: Option[GUI] = None
  
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
   * Initialize the Escalator object. This must be called first.
   */
  def init() {
    handleArgs()
    updateClasspath()
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
    interpreter.initialize() // start it getting ready in the background
  }
  
  private def handleArgs() {
    // Set defaults
    for (source <- config.source) {
      config.target = new File(source, "esc-site")
      config.classpath = source.getAbsolutePath
      config.port = if (showGUI) 8000 else 0
      
      val propFile = new File(source, ".esc-settings")
      if (propFile.exists) {
        val file = new FileInputStream(propFile)
        val props = new java.util.Properties
        props.load(file)
        file.close
        
        if (props.containsKey("target")) {
          config.target = new File(props.getProperty("target"))
        }
        if (props.containsKey("classpath")) {
          config.classpath = props.getProperty("classpath")
        }
        if (props.containsKey("port")) {
          config.port = props.getProperty("port").toInt
        }
      }
    }
  }
  
  def chooseSource() {
    for (g <- gui) {
      import swing.FileChooser._
      
      g.chooser.fileSelectionMode = SelectionMode.DirectoriesOnly
      g.chooser.showDialog(null, "Choose Source Directory") match {
        case Result.Approve => {
          config.source = Some(g.chooser.selectedFile)
        }
        
        case _ => // Ignore
      }
    }
  }
  
  def process() {
    if (!config.source.isDefined) {
      chooseSource()
      if (!config.source.isDefined) return
      handleArgs()
    }
    
    val source = config.source.get
    
    // Check the directories
    if (!source.exists || !source.isDirectory) {
      Console.err.println("Source directory invalid")
      return
    }
    
    if (!config.target.exists) {
      config.target.mkdirs
    }
    
    if (!config.target.exists || !config.target.isDirectory) {
      Console.err.println("Target directory invalid")
      return
    }
    
    // Build the source tree
    val root = TreeNode(source)
    
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
    
      if (gui.isEmpty) {
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