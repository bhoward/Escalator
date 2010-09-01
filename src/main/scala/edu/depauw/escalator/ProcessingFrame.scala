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

import javax.swing.border.TitledBorder

import scala.actors.Actor
import scala.swing._
import Swing._

class ProcessingFrame(gui: GUI) extends MainFrame {
  mainframe =>
  
  val status = new Label("Ready")
  val buttons = new BoxPanel(Orientation.Horizontal)
  
  var interactions: List[InteractionFrame] = Nil

  status.horizontalAlignment = Alignment.Left

    
  val processAction = new Action("Process Source Tree") {
    def apply() {
      // Run this in a separate thread
      Actor.actor {
        Escalator.process()
        status.text = "Ready"
      }
    }
  }
  
  val chooseRootAction = new Action("Choose Project Root") {
    def apply() {
      Escalator.chooseRoot()
    }
  }
  
  val interactAction = new Action("New Interaction Window") {
    accelerator = Some(Util.stroke(java.awt.event.KeyEvent.VK_N))

    def apply() {
      val interaction = new InteractionFrame(gui)
      interactions ::= interaction
      interaction.visible = true
    }
  }
  
  val exitAction = new Action("Exit") {
    def apply() {
      if (queryApply()) {
        mainframe.dispose()
      }
    }
    
    def queryApply(): Boolean = {
      gui.queryExit()
    }
  }

  val aboutAction = new Action("About...") {
    def apply() {
      val about = new AboutFrame
      about.visible = true
    }
  }
  
  def remove(interaction: InteractionFrame) {
    interactions = interactions.filterNot(_ == interaction)
  }
  
  override def closeOperation() {
    if (queryExit()) super.closeOperation()
  }
  
  def queryExit(): Boolean = interactions.forall(_.queryExit())
  
  buttons.contents += new Button(processAction)
  buttons.contents += new Button(interactAction)
  
  title = "Escalator"
  contents = new BorderPanel {
    import BorderPanel.Position._
    
    layout(buttons) = Center
    layout(status) = South
  }
  
  val helpMenu = new HelpMenu(this)
  
  menuBar = new MenuBar {
    contents += new Menu("File") {
      contents += new MenuItem(interactAction)
      
      if (Main.onOSX) {
        OSXHelper.setQuitHandler(exitAction.queryApply())
      } else {
        contents += new Separator
  
        // EXIT: exit the application
        contents += new MenuItem(exitAction)
      }
    }
    
    contents += new Menu("Interact"){
      contents += new MenuItem(chooseRootAction)
      contents += new MenuItem(processAction)
    }
    
    contents += helpMenu
  }
  
  pack()
}