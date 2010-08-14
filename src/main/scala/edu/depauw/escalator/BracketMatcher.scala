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

// BracketMatcher.scala
// Based on Java code written by Joshua Engel:
//   http://www.informit.com/articles/article.aspx?p=31204
// Scala translation by Brian Howard, 

import java.awt.Color
import javax.swing.text._

import scala.swing.event.{CaretUpdate, Event}

object BracketMatcher extends PartialFunction[Event, Unit] {
  val goodPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW)
  val badPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.MAGENTA)
  
  var highlighter: Option[Highlighter] = None
  var start: Option[AnyRef] = None
  var end: Option[AnyRef] = None
  
  def clearHighlights() {
    for (h <- highlighter) {
      for (tag <- start) h.removeHighlight(tag)
      for (tag <- end) h.removeHighlight(tag)
      start = None
      end = None
      highlighter = None
    }
  }
  
  def getCharAt(doc: Document, pos: Int): Char = {
    doc.getText(pos, 1).charAt(0)
  }
  
  // This does nothing about ignoring brackets in quotes or comments...
  def findMatchingBracket(doc: Document, pos: Int): Int = {
    var bracketCount = 0
    var i = pos
    while ((i == pos || bracketCount != 0) && i >= 0) {
      getCharAt(doc, i) match {
        case '(' | '{' | '[' => bracketCount -= 1
        case ')' | '}' | ']' => bracketCount += 1
        case _ => {}
      }
      if (bracketCount != 0) i -= 1
    }
    
    i
  }
  
  def isDefinedAt(e: Event) = e.isInstanceOf[CaretUpdate]
  
  def apply(e: Event) {
    clearHighlights()
    
    val textComponent = e.asInstanceOf[CaretUpdate].source
    
    val source = textComponent.peer
    val doc = source.getDocument
    
    val closePos = textComponent.caret.dot - 1
    if (closePos < 0) return
    
    val closeCh = getCharAt(doc, closePos)
    closeCh match {
      case ')' | '}' | ']' =>
        highlighter = Some(source.getHighlighter)
        val openPos = findMatchingBracket(doc, closePos)
        if (openPos >= 0) {
          val openCh = getCharAt(doc, openPos)
          if ((openCh == '(' && closeCh == ')') ||
              (openCh == '{' && closeCh == '}') ||
              (openCh == '[' && closeCh == ']')) {
            start = Some(highlighter.get.addHighlight(openPos, openPos+1, goodPainter))
            end = Some(highlighter.get.addHighlight(closePos, closePos+1, goodPainter))
          } else {
            start = Some(highlighter.get.addHighlight(openPos, openPos+1, badPainter))
            end = Some(highlighter.get.addHighlight(closePos, closePos+1, badPainter))
          }
        } else {
          end = Some(highlighter.get.addHighlight(closePos, closePos+1, badPainter))
        }
      case _ => {}
    }
  }
}
