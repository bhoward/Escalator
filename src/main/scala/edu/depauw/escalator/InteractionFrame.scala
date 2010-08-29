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
import java.awt.{Font => AWTFont}
import java.awt.event.KeyEvent._

import javax.swing.WindowConstants
import javax.swing.undo.UndoManager
import javax.swing.text.DefaultEditorKit
import javax.swing.event.{UndoableEditListener, UndoableEditEvent}

import scala.actors.Actor
import scala.swing._
import Swing._
import event._

class InteractionFrame(val gui: GUI) extends Frame { frame =>
  val source = new TextArea
  val output = new TextArea
  
  var _file: Option[File] = None
  var _modified = false
  
  def file = _file
  def file_=(f: Option[File]) {
    _file = f
    setTitle()
  }
  
  def modified = _modified
  def modified_=(b: Boolean) {
    if (b != _modified) {
      _modified = b
      setTitle()
    }
  }
  
  def setTitle() {
    val fileName = if (file.isDefined) {
      file.get.getPath
    } else {
      "untitled"
    }
    
    title = "Escalator: " + fileName
    if (GUIMain.onOSX) {
      peer.getRootPane.putClientProperty("Window.documentModified", modified)
    } else {
      if (modified) {
        title = "* " + title
      }
    }
  }
  
  val split = new SplitPane(Orientation.Vertical,
      new ScrollPane(source), new ScrollPane(output))
  
  val SmallFont = new AWTFont("Monospaced", AWTFont.PLAIN, 14)
  val LargeFont = new AWTFont("Monospaced", AWTFont.BOLD, 24)
  
  def setFont(font: AWTFont) {
    source.font = font
    output.font = font
  }
  
  peer.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE)
  
  override def closeOperation() {
    queryExit()
  }
  
  /**
   * Offers the chance to save if modified; returns true if OK to proceed, else cancel.
   */
  def querySave(): Boolean = !modified || {
    import Dialog._
    
    showConfirmation(split, "Save before closing?", optionType = Options.YesNoCancel) match {
      case Result.Yes => saveAction.apply(); true
      
      case Result.No => true
      
      case Result.Cancel => return false
    }
  }
  
  def queryExit(): Boolean = querySave() && {
    gui.mainframe.remove(this)
    this.dispose()
    true
  }
  
  // Set some properties
  setFont(SmallFont)
  output.editable = false
  split.resizeWeight = 0.5
  
  source.caret.reactions += BracketMatcher
  source.keys.reactions += {
    case e @ KeyPressed(_, Key.Enter, mod, _) =>
      if (mod == 0) {
        AutoIndent(e.peer)
        e.consume()
      } else if (mod == Key.Modifier.Control) {
        // Ctrl+Enter runs the example
        runAction()
      }
  }
  
  def getIcon(name: String) = new javax.swing.ImageIcon(getClass.getResource(name))
  
  val newScalaAction = new Action("New .scala") {
    icon = getIcon("/toolbarButtonGraphics/general/New16.gif")
    toolTip = "New Scala Script"
    accelerator = Some(Util.stroke(VK_N))
    
    def apply() {
      if (querySave()) {
        source.text = ""
        output.text = ""
        file = None
        modified = false
        setScalaMode()
      }
    }
  }

  val newEscalatorAction = new Action("New .esc") {
    def apply() {
      if (querySave()) {
        source.text = ""
        output.text = ""
        file = None
        modified = false
        setEscalatorMode()
      }
    }
  }

  val openAction = new Action("Open...") {
    icon = getIcon("/toolbarButtonGraphics/general/Open16.gif")
    toolTip = "Open..."
    
    def apply() {
      if (querySave()) {
        import FileChooser._
        
        gui.chooser.fileSelectionMode = SelectionMode.FilesOnly
        gui.chooser.showOpenDialog(split) match {
          case Result.Approve =>
            val newFile = gui.chooser.selectedFile
            source.text = Util.readFile(newFile)
            output.text = ""
            file = Some(newFile)
            modified = false
            if (newFile.getName endsWith ".esc") {
              setEscalatorMode()
            } else {
              setScalaMode()
            }
          
          case _ => // Ignore
        }
      }
    }
  }

  val saveAction: Action = new Action("Save") {
    icon = getIcon("/toolbarButtonGraphics/general/Save16.gif")
    toolTip = "Save"
    accelerator = Some(Util.stroke(VK_S))
    
    def apply() {
      if (file.isDefined) {
        Util.writeFile(file.get, source.text)
        modified = false
      } else {
        saveAsAction.apply()
      }
    }
  }

  val saveAsAction = new Action("Save As...") {
    accelerator = Some(Util.altStroke(VK_S))
    
    def apply() {
      import FileChooser._
      
      gui.chooser.showSaveDialog(split) match {
        case Result.Approve =>
          gui.chooser.fileSelectionMode = SelectionMode.FilesOnly 
          val newFile = gui.chooser.selectedFile
          if (!file.isDefined || file.get != newFile) {
            if (newFile.exists) {
              import Dialog._
              
              showConfirmation(split, "Overwrite existing file?") match {
                case Result.Yes => {}
                case Result.No => return
              }
            }
            
            file = Some(newFile)
          }
          saveAction.apply()
        
        case _ => // Ignore
      }
    }
  }

  val closeAction = new Action("Close") {
    accelerator = Some(Util.stroke(VK_W))
    
    def apply() {
      queryExit()
    }
  }

  val exitAction = new Action("Exit") {
    def apply() {
      if (queryApply()) {
        gui.mainframe.dispose()
      }
    }
    
    def queryApply(): Boolean = {
      gui.queryExit()
    }
  }

  val runAction = new Action("Run") {
    icon = getIcon("/toolbarButtonGraphics/media/Play16.gif")
    toolTip = "Run Scala Code"
    
    def apply() {
      Actor.actor {
        Escalator.interpreter.reset()
        output.text = Escalator.runExample(getSource(source.text))
      }
    }
  }
  
  val fontSmallAction = new Action("Small Font") {
    icon = getIcon("/toolbarButtonGraphics/general/ZoomOut16.gif")
    toolTip = "Small Font"
    accelerator = Some(Util.stroke(VK_MINUS))
    
    def apply() {
      setFont(SmallFont)
    }
  }
  
  val fontLargeAction = new Action("Large Font") {
    icon = getIcon("/toolbarButtonGraphics/general/ZoomIn16.gif")
    toolTip = "Large Font"
    accelerator = Some(Util.stroke(VK_PLUS))
    
    def apply() {
      setFont(LargeFont)
    }
  }
  
  val undo = new UndoManager()
  
  def updateUndoRedo() {
    undoAction.enabled = undo.canUndo
    redoAction.enabled = undo.canRedo
    
    undoAction.title = if (undo.canUndo) {
      undo.getUndoPresentationName
    } else {
      "Undo"
    }
    redoAction.title = if (undo.canRedo) {
      undo.getRedoPresentationName
    } else {
      "Redo"
    }
  }
  
  val undoAction = new Action("Undo") {
    icon = getIcon("/toolbarButtonGraphics/general/Undo16.gif")
    toolTip = "Undo"
    accelerator = Some(Util.stroke(VK_Z))
    
    enabled = false

    def apply() {
      undo.undo()
      updateUndoRedo()
    }
  }

  val redoAction = new Action("Redo") {
    icon = getIcon("/toolbarButtonGraphics/general/Redo16.gif")
    toolTip = "Redo"
    accelerator = Some(Util.stroke(VK_Y))
    
    enabled = false

    def apply() {
      undo.redo()
      updateUndoRedo()
    }
  }
  
  val cutAction = new Action("Cut") {
    icon = getIcon("/toolbarButtonGraphics/general/Cut16.gif")
    toolTip = "Cut to Clipboard"
    
    override lazy val peer = new DefaultEditorKit.CutAction()
    def apply() {}
  }
  
  val copyAction = new Action("Copy") {
    icon = getIcon("/toolbarButtonGraphics/general/Copy16.gif")
    toolTip = "Copy to Clipboard"
    
    override lazy val peer = new DefaultEditorKit.CopyAction()
    def apply() {}
  }
  
  val pasteAction = new Action("Paste") {
    icon = getIcon("/toolbarButtonGraphics/general/Paste16.gif")
    toolTip = "Paste from Clipboard"
    
    override lazy val peer = new DefaultEditorKit.PasteAction()
    def apply() {}
  }
  
  val selectAllAction = new Action("Select All") {
    override lazy val peer = source.peer.getActionMap().get(DefaultEditorKit.selectAllAction)
    def apply() {}
  }
  
  val aboutAction = new Action("About...") {
    def apply() {
      val about = new AboutFrame
      about.visible = true
    }
  }
  
  val toEscAction: Action = new Action("Convert to .esc") {
    def apply() {
      if (source.text.trim != "") {
        // Only wrap script as an example if there is a script...
        if (!source.text.endsWith("\n")) source.text += "\n"
        source.text = "//example\n" + source.text + "//end example\n"
      }
      file = None // force a Save As...
      setEscalatorMode()
    }
  }
  
  val toScalaAction = new Action("Convert to .scala") {
    def apply() {
      import Dialog._
      
      showConfirmation(split, "OK to delete non-code content?") match {
        case Result.Ok =>
          source.text = extract(source.text)
          file = None
          setScalaMode()
          
        case _ => // Ignore
      }
    }
  }
  
  val convertButton = new Button(toEscAction)
  
  var getSource: String => String = id
  
  def setScalaMode() {
    toEscAction.enabled = true
    convertButton.action = toEscAction
    toScalaAction.enabled = false
    getSource = id
  }

  def setEscalatorMode() {
    toScalaAction.enabled = true
    convertButton.action = toScalaAction
    toEscAction.enabled = false
    getSource = extract
  }

  def id(text: String) = text
  
  val Example = """(?s).*?//[ \t]*example[ \t]*\n(.*?)//[ \t]*end[ \t]+example[ \t]*\n(.*)""".r
  
  def extract(doc: String): String = doc match {
    case Example(code, rest) => code + extract(rest)
    case _ => ""
  }
    
  val doc = source.peer.getDocument
  doc.addUndoableEditListener(new UndoableEditListener {
    def undoableEditHappened(e: UndoableEditEvent) {
      //Remember the edit and update the menus.
      undo.addEdit(e.getEdit())
      updateUndoRedo()
      modified = true
    }
  })
  
  setScalaMode()
  
  val toolbar = new Component with SequentialContainer.Wrapper {
    override lazy val peer= new javax.swing.JToolBar
    
    contents += new Button(openAction) { text = "" }
    contents += new Button(saveAction) { text = "" }
    peer.addSeparator()
// These are commented out because I don't see how to keep the text empty
//    contents += new Button(undoAction) { text = "" }
//    contents += new Button(redoAction) { text = "" }
    contents += new Button(cutAction) { text = "" }
    contents += new Button(copyAction) { text = "" }
    contents += new Button(pasteAction) { text = "" }
    peer.addSeparator()
    contents += new Button(fontSmallAction) { text = "" }
    contents += new Button(fontLargeAction) { text = "" }
    peer.addSeparator()
    contents += new Button(runAction) { text = "" }
    peer.addSeparator()
    contents += convertButton
  }

  file = None // Force the initial title
  contents = new BorderPanel {
    import BorderPanel.Position._
    
    layout(toolbar) = North
    layout(split) = Center
  }
  
  menuBar = new EscalatorMenu(this)
  
  preferredSize = (800, 600)
  pack()
  centerOnScreen()
}