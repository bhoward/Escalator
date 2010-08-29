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
import javax.swing.KeyStroke

import scala.xml.NodeSeq

import org.mozilla.{javascript => js}

object Util {
  val shortcutMask = java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()
  def stroke(keyCode : Int) = KeyStroke.getKeyStroke(keyCode, shortcutMask)
  def altStroke(keyCode : Int) = KeyStroke.getKeyStroke(keyCode, shortcutMask + java.awt.event.InputEvent.ALT_MASK)

  /**
   * This function opens and prepares all the text in the file for parsing.
   * No parsing is done here in this function, though.
   * 
   * @return all text contained within the desired file.  If the
   * file can't be opened (i.e. it doesn't exist), then it just returns a
   * massage saying it couldn't find the path provided.
   */
  def readFile(file: File): String = {
    try {
      scala.io.Source.fromFile(file).mkString
    } catch { case _ => "[!file not found: "+file+"!]" }
  }

  def writeFile(file: File, text: String) {
    try {
      val fw = new FileWriter( file )
      fw.write( text, 0, text.length() )
      fw.close()
    } catch { case ioe => println(ioe) }
  }

  /**
   * Copy a file.
   * 
   * @param from The original file
   * @param to The new copy
   */
  def copyFile(from: File, to: File) {
    val in = new FileInputStream(from)
    val out = new FileOutputStream(to)
    val buffer = new Array[Byte](1024)
    
// This is slicker, but only in 2.8:
//    Iterator.continually(in.read(buffer))
//            .takeWhile(_ != -1)
//            .foreach { out.write(buffer, 0 , _) }
                         
    var n = in.read(buffer)
    while (n != -1) {
      out.write(buffer, 0, n)
      n = in.read(buffer)
    }
    
    in.close()
    out.close()
  }
  
  val Quoted = """^(.*?)'((?:\\'|[^'])*?)'(.*)$""".r
  
  /**
   * Remove matched single quotes from a string if present, and unescape contained quotes.
   */
  def unquote(s: String): String = s match {
    case Quoted(pre, in, post) => pre + in.replace("\\'", "'") + unquote(post)
    case _ => s
  }
  
  def baseExtension(name: String): (String, String) = {
    val pos = name.lastIndexOf(".")
    
    if (pos >= 0) {
      (name.substring(0, pos), name.substring(pos+1))
    } else {
      (name, "")
    }
  }
  
  private lazy val (scope, converter) = {
    // Initialize the Javascript environment
    val ctx = js.Context.enter
    try {
      val scope = ctx.initStandardObjects
      
      // Open the Showdown script and evaluate it in the Javascript context.

      val showdownURL = getClass.getResource("/site/script/showdown.js")
      val stream = new InputStreamReader(showdownURL.openStream)
      ctx.evaluateReader(scope, stream, "showdown", 1, null)

      // Instantiate a new Showdown converter.

      val converterCtor = ctx.evaluateString(scope, "Showdown.converter", "converter", 1, null).asInstanceOf[js.Function]
      (scope, converterCtor.construct(ctx, scope, null))
    } finally {
      js.Context.exit
    }
  }
  
  /**
   * Convert markdown to HTML. Based on http://brizzled.clapper.org/id/pr-98.html
   * 
   * @param markdownSource
   */
  def markdown(markdownSource: String): String = {
    // Initialize the Javascript environment
    val ctx = js.Context.enter
    try {
      // Get the function to call.

      val makeHTML = converter.get("makeHtml", converter).asInstanceOf[js.Function]

      // Convert the markdown source to HTML.

      val htmlBody = makeHTML.call(ctx, scope, converter, Array[Object](markdownSource))
      htmlBody.toString
    } finally {
      js.Context.exit
    }
  }
  
  /**
   * Write an HTML page to the given file.
   * 
   * @param file
   * @param title The title for the page
   * @param head Extra content for the head section
   * @param body Content for the body of the page
   */
  def writeHTML(file: File, title: String, head: NodeSeq, body: NodeSeq) {
    val doctype = """<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">"""
    val page =
      <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
        <head>
          <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
          <title>{title}</title>
          { head }
        </head>
        <body>
          { body }
        </body>
      </html>
         
    val writer = new PrintWriter(new FileWriter(file))
    writer.println(doctype)
    writer.println(page)
    writer.close()
  }
}