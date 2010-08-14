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

import javax.swing.JEditorPane
import javax.swing.text.html.HTMLEditorKit

import scala.swing._

class AboutFrame extends Frame() {
  preferredSize = new Dimension(400, 400)
  // peer.setResizable( false )
  title = "About Escalator"
  val mainPanel = new BorderPanel
  val flowPanel = new FlowPanel {
    contents +=  new Button("OK") {
      reactions += {
        case swing.event.ButtonClicked(_) => dispose()
      }
    }
  }

  object aboutPane extends Component {
    override lazy val peer = new JEditorPane() {
      setContentType("text/html")
      setEditorKit(new HTMLEditorKit())
//      val tempField = new JTextField()
//      setBorder(tempField.getBorder())
      setText("<html>" + "<body>" + "Escalator" + "<br>" +
        "version 2.0 RC1" + "<br>" + "<br>" +
        "Developed at DePauw University by" + "<br>" +
        "Emily Bruckart, Nathan Bude, and Dr. Brian Howard" + "<br>" +
        "Copyright &copy; 2010, Brian T. Howard" + "<br>" +
        "Supported in part by NSF REU Grant number CCF-0851812")

      setEditable( false )
    }
  }

  mainPanel.layout(aboutPane) = BorderPanel.Position.Center
  mainPanel.layout(flowPanel) = BorderPanel.Position.South
  contents = mainPanel
  
  pack()
}