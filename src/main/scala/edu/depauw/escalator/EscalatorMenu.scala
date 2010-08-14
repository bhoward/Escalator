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

import swing._
import swing.event._

import javax.swing.text._
import javax.swing.undo._
import javax.swing.{AbstractAction, KeyStroke}
import java.awt.event.KeyEvent._

import com.centerkey.utils.BareBonesBrowserLaunch

////////////////////////////
//////  MENUS - GUI  ///////
////////////////////////////
class EscalatorMenu(frame: InteractionFrame) extends MenuBar {
  val shortcutMask = java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()
  def stroke(keyCode : Int) = KeyStroke.getKeyStroke(keyCode, shortcutMask)
  def altStroke(keyCode : Int) = KeyStroke.getKeyStroke(keyCode, shortcutMask + java.awt.event.InputEvent.ALT_MASK)
  
  /////////////////////////
  // 1. FILE OPERATIONS  //
  /////////////////////////
  contents += new Menu("File") {
    // NEW: create a new file
    contents += new MenuItem(frame.newAction) {
      peer.setAccelerator(stroke(VK_N))
    }

    // OPEN: open a file
    contents += new MenuItem(frame.openAction)

    // SAVE: save the current file
    contents += new MenuItem(frame.saveAction) {
      peer.setAccelerator(stroke(VK_S))
    }

    // SAVE AS: save the current file under the name specified by the user
    contents += new MenuItem(frame.saveAsAction) {
      peer.setAccelerator(altStroke(VK_S))
    }

    contents += new Separator

    // CLOSE: close the current document
    contents += new MenuItem(frame.closeAction){
      peer.setAccelerator(stroke(VK_W))
    }

    if (GUIMain.onOSX) {
      OSXHelper.setQuitHandler(frame.exitAction.queryApply())
    } else {
      contents += new Separator

      // EXIT: exit the application
      contents += new MenuItem(frame.exitAction)
    }
  }

  ///////////////////////
  // 2. TEXT EDITING   //
  ///////////////////////
  contents += new Menu("Edit"){
    contents += new MenuItem(frame.undoAction) {
      peer.setAccelerator(stroke(VK_Z))
      mnemonic = Key.U
    }
    contents += new MenuItem(frame.redoAction) {
      peer.setAccelerator(stroke(VK_Y))
      mnemonic = Key.R
    }
    contents += new Separator
    contents += new MenuItem("Cut") {
      peer.setAction(new DefaultEditorKit.CutAction())
      text = "Cut"
      mnemonic = Key.T
    }
    contents += new MenuItem("Copy") {
      peer.setAction(new DefaultEditorKit.CopyAction())
      text = "Copy"
      mnemonic = Key.C
    }
    contents += new MenuItem("Paste") {
      peer.setAction(new DefaultEditorKit.PasteAction())
      text = "Paste"
      mnemonic = Key.P
    }
    contents += new MenuItem("Select All") {
      peer.setAction(frame.source.peer.getActionMap().get(DefaultEditorKit.selectAllAction))
      text = "Select All"
      mnemonic = Key.A
    }
  }
  
  ///////////////////////
  // 3. INTERACTION    //
  ///////////////////////
  contents += new Menu("Interact"){
    contents += new MenuItem(frame.runAction)
    contents += new Separator
    contents += new MenuItem(frame.toEscAction)
    contents += new MenuItem(frame.toScalaAction)
    contents += new Separator
    contents += new MenuItem(frame.fontSmallAction)
    contents += new MenuItem(frame.fontLargeAction)
  }
  
  ///////////////////////
  // 4. HELP/ABOUT     //
  ///////////////////////
  contents += new Menu("Help"){
    contents += new MenuItem("Documentation") {
      reactions += {
        case ButtonClicked(_) => BareBonesBrowserLaunch.openURL("http://scales.csc.depauw.edu/Escalator/Documentation.html")
      }
    }
    contents += new Separator
    if (GUIMain.onOSX) {
      OSXHelper.setAboutHandler(frame.aboutAction.apply())
    } else {
      contents += new MenuItem(frame.aboutAction)
    }
    contents += new MenuItem("Escalator Home") {
      reactions += {
        case ButtonClicked(_) => BareBonesBrowserLaunch.openURL("http://scales.csc.depauw.edu/Escalator/")
      }
    }
    contents += new Menu("Bug Reporting") {
      contents += new MenuItem("New ticket") {
        reactions += {
          case swing.event.ButtonClicked(_) => BareBonesBrowserLaunch.openURL("http://twiki.csc.depauw.edu/projects/scales/newticket")
        }
      }
      contents += new MenuItem("View existing tickets") {
        reactions += {
          case swing.event.ButtonClicked(_) => BareBonesBrowserLaunch.openURL("http://twiki.csc.depauw.edu/projects/scales/report")
        }
      }
    }
  }
}
