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

import scala.swing._
import event.ButtonClicked

import com.centerkey.utils.{BareBonesBrowserLaunch => BBBL}

class HelpMenu(mainframe: ProcessingFrame) extends Menu("Help") {
  contents += new MenuItem("Documentation") {
    reactions += {
      case ButtonClicked(_) => BBBL.openURL("http://scales.csc.depauw.edu/tut/EscalatorDoc.html")
    }
  }
  
  contents += new Separator
  
  if (Main.onOSX) {
    OSXHelper.setAboutHandler(mainframe.aboutAction.apply())
  } else {
    contents += new MenuItem(mainframe.aboutAction)
  }
  
  contents += new MenuItem("Escalator Home") {
    reactions += {
      case ButtonClicked(_) => BBBL.openURL("http://scales.csc.depauw.edu/")
    }
  }
  
  contents += new Menu("Bug Reporting") {
    contents += new MenuItem("New ticket") {
      reactions += {
        case ButtonClicked(_) => BBBL.openURL("http://twiki.csc.depauw.edu/projects/scales/newticket")
      }
    }
    contents += new MenuItem("View existing tickets") {
      reactions += {
        case ButtonClicked(_) => BBBL.openURL("http://twiki.csc.depauw.edu/projects/scales/report")
      }
    }
  }
}