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

import java.io._

/**
 * This is like a StringWriter, but for byte streams
 */
class EscOutputStream extends OutputStream {
  private val buffer = new StringBuilder
  
  def string = buffer.toString
  
  def write(b: Int) {
    buffer.append(b.asInstanceOf[Char])
  }
  
  def clear() {
    buffer.delete(0, buffer.length)
  }
}
