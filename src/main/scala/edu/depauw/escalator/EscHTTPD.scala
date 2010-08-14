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
import java.net.URL
import java.util.Properties

import fi.iki._

/**
 * Specialized version of NanoHTTPD:
 * 
 *  * serves /resource URLs from resource directory, /project (and /) URLs from project
 *  
 * @author Brian Howard
 */
class EscHTTPD(port: Int, resourceHome: String, project: File) extends NanoHTTPD(port) {
  val Resource = """/resource(/.*)""".r
  val Project = """/project(/.*)""".r
  val Console = """/console/(.*)""".r
  
  override def serve(uri: String, method: String, header: Properties, params: Properties): Response = {
    uri match {
      case Resource(rest) => serveURL(rest, header, resourceHome)
      case Project(rest) => serveFile(rest, header, project, true)
      case Console(mode) => serveConsole(mode, params.getProperty("input"))
      case _ => serveFile(uri, header, project, true)
    }
  }
  
  def serveConsole(mode: String, code: String): Response = {
    Escalator.interpreter.reset()
    val result = if (mode == "test") {
      <pre class="brush: specs; light: true;">{ Escalator.runTest(code) }</pre>
    } else {
      <pre class="brush: plain; light: true;">{ Escalator.runExample(code) }</pre>
    }
    
    new Response(NanoHTTPD.HTTP_OK, NanoHTTPD.MIME_HTML, result.toString)
  }
  
  /**
   * Simplified version of NanoHTTPD.serveFile (crudely translated from Java).
   * Serves file from homeURL and its' subdirectories (only).
   * Uses only URI, ignores all headers and HTTP parameters.
   */
  def serveURL(origURI: String, header: Properties, home: String): Response = {
    // Remove URL arguments
    var uri = origURI.trim.replace( File.separatorChar, '/' )
    if ( uri.indexOf( '?' ) >= 0 )
      uri = uri.substring(0, uri.indexOf( '?' ))

    // Prohibit getting out of current directory
    if ( uri.startsWith( ".." ) || uri.endsWith( ".." ) || uri.indexOf( "../" ) >= 0 )
      return new Response( NanoHTTPD.HTTP_FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT,
                 "FORBIDDEN: Won't serve ../ for security reasons." )

    val u = getClass.getResource(home + uri)
    
    // Get MIME type from file name extension, if possible
    var mime: String = null
    val dot = u.getPath.lastIndexOf( '.' )
    if ( dot >= 0 )
      mime = NanoHTTPD.theMimeTypes.get( u.getPath.substring( dot + 1 ).toLowerCase)
    if ( mime == null )
      mime = NanoHTTPD.MIME_DEFAULT_BINARY

    val is = u.openStream()
    new Response( NanoHTTPD.HTTP_OK, mime, is )
  }
}