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

import java.io.File

/**
 * Application starting point for Escalator.
 * 
 * Usage:
 * scala edu.depauw.escalator.Main [-gui] [root]
 * - no arguments, or with -gui, starts a GUI
 * - root is project root directory to be processed
 * Other config info goes in root/.esc-settings
 * 
 * @author Brian Howard
 */
object Main {
  val onOSX = System.getProperty("mrj.version") != null
  
  def main(args: Array[String]) {
    Escalator.showGUI = args.isEmpty
    var realArgs = args
    
    if (realArgs.size > 0 && realArgs(0) == "-gui") {
      realArgs = realArgs.slice(1, realArgs.size)
      Escalator.showGUI = true
    }
    
    if (realArgs.isEmpty) {
      Escalator.config.root = None
    } else {
      Escalator.config.root = Some(new File(realArgs(0)))
    }
    
    Escalator.init()
    
    if (Escalator.showGUI) {
      if (onOSX) {
        System.setProperty("apple.laf.useScreenMenuBar", "true")
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Escalator")
      }
      
      swing.Swing.onEDT {
        Escalator.gui = Some(new GUI)
      }
    } else {
      Escalator.gui = None
      Escalator.process()
    }
  }
}