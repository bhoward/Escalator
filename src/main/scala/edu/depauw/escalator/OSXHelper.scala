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

import com.apple.eawt._

object OSXHelper {
  lazy val app = Application.getApplication
  
  var quitHandler : ApplicationListener = null
  var aboutHandler : ApplicationListener = null
  var preferencesHandler : ApplicationListener = null
  var openFileHandler : ApplicationListener = null
  
  def setQuitHandler(action : => Boolean) {
    if (quitHandler != null) app.removeApplicationListener(quitHandler)
    quitHandler = new ApplicationAdapter {
      override def handleQuit(event : ApplicationEvent) {
        val handled = action
        event.setHandled(handled)
      }
    }
    app.addApplicationListener(quitHandler)
  }
  
  def setAboutHandler(action : => Unit) {
    if (aboutHandler != null) app.removeApplicationListener(aboutHandler)
    aboutHandler = new ApplicationAdapter {
      override def handleAbout(event : ApplicationEvent) {
        action
        event.setHandled(true)
      }
    }
    app.addApplicationListener(aboutHandler)
  }
  
  def setPreferencesHandler(action : => Unit) {
    if (preferencesHandler != null) app.removeApplicationListener(preferencesHandler)
    preferencesHandler = new ApplicationAdapter {
      override def handlePreferences(event : ApplicationEvent) {
        action
        event.setHandled(true)
      }
    }
    app.addApplicationListener(preferencesHandler)
  }
  
  def setOpenFileHandler(action : String => Unit) {
    if (openFileHandler != null) app.removeApplicationListener(openFileHandler)
    openFileHandler = new ApplicationAdapter {
      override def handleOpenFile(event : ApplicationEvent) {
        action(event.getFilename)
        event.setHandled(true)
      }
    }
    app.addApplicationListener(openFileHandler)
  }
}
