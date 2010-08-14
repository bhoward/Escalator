package edu.depauw.escalator

/**
 * MySwing: Advanced Swing Utilites
 * Copyright (C) 2005  Santhosh Kumar T
 * (Minimally) Translated to Scala by Brian Howard
 * Source: http://www.jroller.com/santhosh/entry/autoindent_for_jtextarea
 * <p/>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */

import javax.swing._
import javax.swing.text.BadLocationException
import java.awt.event.KeyEvent

/**
 * @author Santhosh Kumar T
 * @email santhosh@fiorano.com
 */
object AutoIndent {
  def apply(ae: KeyEvent) {
    val comp = ae.getSource.asInstanceOf[JTextArea]
    val doc = comp.getDocument

    if (!comp.isEditable)
      return
    try {
      val line = comp.getLineOfOffset(comp.getCaretPosition)

      val start = comp.getLineStartOffset(line)
      val end = comp.getLineEndOffset(line)
      val str = doc.getText(start, end - start - 1)
      val whiteSpace = getLeadingWhiteSpace(str)
      doc.insertString(comp.getCaretPosition, '\n' + whiteSpace, null)
    } catch {
      case ex: BadLocationException =>
        doc.insertString(comp.getCaretPosition, "\n", null)
    }
  }

  /**
   *  Returns leading white space characters in the specified string.
   */
  def getLeadingWhiteSpace(str : String) : String = {
    (str.takeWhile {ch => Character.isWhitespace(ch)}).mkString
  }
} 