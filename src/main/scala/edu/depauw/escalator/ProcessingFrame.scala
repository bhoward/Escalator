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
  val status = new Label("Ready")
  val buttons = new BoxPanel(Orientation.Horizontal)
  
  var interactions: List[InteractionFrame] = Nil

  status.horizontalAlignment = Alignment.Left

// TODO put all this in a preferences pane, along with choosing port number (or no server)
//  val chooseSourceAction = new Action("Set Source") {
//    def apply() {
//      import FileChooser._
//      
//      gui.chooser.fileSelectionMode = SelectionMode.DirectoriesOnly
//      gui.chooser.showDialog(panel, "Choose Source Directory") match {
//        case Result.Approve => {
//          Escalator.setSource(gui.chooser.selectedFile)
//        }
//        
//        case _ => // Ignore
//      }
//    }
//  }
//  
//  val chooseTargetAction = new Action("Set Target") {
//    def apply() {
//      import FileChooser._
//      
//      gui.chooser.fileSelectionMode = SelectionMode.DirectoriesOnly
//      gui.chooser.showDialog(panel, "Choose Target Directory") match {
//        case Result.Approve => {
//          Escalator.setTarget(gui.chooser.selectedFile)
//        }
//        
//        case _ => // Ignore
//      }
//    }
//  }
//  
//  val chooseClasspathAction = new Action("Set Classpath") {
//    def apply() {
//      import FileChooser._
//      
//      gui.chooser.fileSelectionMode = SelectionMode.FilesAndDirectories
//      // TODO allow multiSelection? filter to only directories and jars?
//      gui.chooser.showDialog(panel, "Choose Classpath") match {
//        case Result.Approve => {
//          Escalator.setClasspath(gui.chooser.selectedFile.getAbsolutePath)
//        }
//        
//        case _ => // Ignore
//      }
//    }
//  }
    
  val processAction = new Action("Process Source Tree") {
    def apply() {
      // Run this in a separate thread
      Actor.actor {
        Escalator.process()
        status.text = "Ready"
      }
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
  
  pack()
}