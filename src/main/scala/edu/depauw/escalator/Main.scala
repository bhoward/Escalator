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

import org.github.scopt._

/**
 * Application starting point for the Escalator command-line tool.
 * 
 * Usage:
 * scala edu.depauw.escalator.Main [source [target]]
 * - source is directory containing source files to be processed;
 *     default is current directory
 * - target is directory to contain generated tree of html files;
 *     default is source/esc-site
 * 
 * @author Brian Howard
 */
object Main {
  def main(args: Array[String]) {
    Escalator.init(args, None)
    
    Escalator.process()
    
    println("Ready - Press Return to Quit")
    System.in.read()
  }
}