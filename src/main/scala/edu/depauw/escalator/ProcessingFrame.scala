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
  val panel = new BoxPanel(Orientation.Vertical)
  val status = new Label("Ready")
  val sourceLabel = new Label(Escalator.getSource.getAbsolutePath)
  val targetLabel = new Label(Escalator.getTarget.getAbsolutePath)
  val classpathLabel = new Label(Escalator.getClasspath)
  val buttons = new BoxPanel(Orientation.Horizontal)
  
  var interactions: List[InteractionFrame] = Nil

  status.horizontalAlignment = Alignment.Left
  sourceLabel.horizontalAlignment = Alignment.Left
  targetLabel.horizontalAlignment = Alignment.Left

  val chooseSourceAction = new Action("Set Source") {
    def apply() {
      import FileChooser._
      
      gui.chooser.fileSelectionMode = SelectionMode.DirectoriesOnly
      gui.chooser.showDialog(panel, "Choose Source Directory") match {
        case Result.Approve => {
          Escalator.setSource(gui.chooser.selectedFile)
          sourceLabel.text = gui.chooser.selectedFile.getAbsolutePath
        }
        
        case _ => // Ignore
      }
    }
  }
  
  val chooseTargetAction = new Action("Set Target") {
    def apply() {
      import FileChooser._
      
      gui.chooser.fileSelectionMode = SelectionMode.DirectoriesOnly
      gui.chooser.showDialog(panel, "Choose Target Directory") match {
        case Result.Approve => {
          Escalator.setTarget(gui.chooser.selectedFile)
          targetLabel.text = gui.chooser.selectedFile.getAbsolutePath
        }
        
        case _ => // Ignore
      }
    }
  }
  
  val chooseClasspathAction = new Action("Set Classpath") {
    def apply() {
      import FileChooser._
      
      gui.chooser.fileSelectionMode = SelectionMode.FilesAndDirectories
      // TODO allow multiSelection? filter to only directories and jars?
      gui.chooser.showDialog(panel, "Choose Classpath") match {
        case Result.Approve => {
          Escalator.setClasspath(gui.chooser.selectedFile.getAbsolutePath)
          classpathLabel.text = gui.chooser.selectedFile.getAbsolutePath
        }
        
        case _ => // Ignore
      }
    }
  }
    
  val processAction = new Action("Process") {
    def apply() {
      // Run this in a separate thread
      Actor.actor {
        Escalator.process()
        status.text = "Ready"
      }
    }
  }
  
  val interactAction = new Action("Interact") {
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
  
  val sourceRow = new BoxPanel(Orientation.Horizontal)
  sourceRow.contents += new Button(chooseSourceAction)
  sourceRow.contents += sourceLabel
  sourceRow.contents += Glue
  sourceRow.border = new TitledBorder("Project Source Directory")
  
  val targetRow = new BoxPanel(Orientation.Horizontal)
  targetRow.contents += new Button(chooseTargetAction)
  targetRow.contents += targetLabel
  targetRow.contents += Glue
  targetRow.border = new TitledBorder("HTML Target Directory")
  
  val classpathRow = new BoxPanel(Orientation.Horizontal)
  classpathRow.contents += new Button(chooseClasspathAction)
  classpathRow.contents += classpathLabel
  classpathRow.contents += Glue
  classpathRow.border = new TitledBorder("Project Classpath")
  
  panel.contents += sourceRow
  panel.contents += targetRow
  panel.contents += classpathRow
  panel.contents += buttons
  
  title = "Escalator"
  contents = new BorderPanel {
    import BorderPanel.Position._
    
    layout(panel) = Center
    layout(status) = South
  }
  
  pack()
}