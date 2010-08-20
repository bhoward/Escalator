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

object Find {
  val DECLARATION = """\b(package|type|object|class|trait|def|val|var)\b"""
  val COMMENT = """(//|/\*|\*)"""
  val QUOTE = """'(\\'|.)*?'"""
  
  /**
   * Calculate an indentation depth score for the given line. A tab counts
   * as 16 spaces (don't use tabs...), and a curly brace counts as 1/16th of
   * a space (so the surrounding braces are still treated as indented, but not
   * by much).
   */
  def depth(line: String): Int = {
    val indent = "^[ \t]*[\\{\\}\\(\\)\\[\\]]?".r.findAllIn(line).next
    """[\{\}\(\)\[\]]""".r.findAllIn( indent ).length + (
      16 * (" ".r.findAllIn( indent ).length + (
	    16 * "\t".r.findAllIn( indent ).length)))
  }
  
  def toRegex(s: String): String = s match {
    case Util.Quoted(pre, in, post) =>
      pre.replace(" ", ".*").replace("~", DECLARATION) +
        "\\Q" + in.replace("\\'", "'") + "\\E" + toRegex(post)
    case _ => s.replace(" ", ".*").replace("~", DECLARATION)
  }

  def follow(selval: List[String], lines: List[String]): (Int, Int) = {
    var selector = selval // can't make the argument variable
    val reluctant = true // captures preceding consecutive commented lines when true
    var (start, end) = (0, -1)
    var startDepth = 0
    
    for( (line, numline) <- lines.zip( 0.until(lines.length) ) ) {
      if( ("^\\s*("+COMMENT+".*)?$").r.findFirstMatchIn( line ) == None ) {
        if( 0 < selector.length ) {
          if( (toRegex( selector(0) )).r.findFirstIn( line ) != None ) {
            selector = selector.drop(1)
            if( 0 == selector.length ) {
              start = if( reluctant ) end+1 else numline
              startDepth = depth(line)
            }
          }
        } else {
          if( depth(line) <= startDepth )
            return (start, end)
        }
        end = numline
      } else if( 0 < selector.length && "^\\s*$".r.findFirstMatchIn( line ) != None )
        end = numline
    }
    (start, end)
  }
  
  def apply( file: File, query: String ): Seq[(String, Int)] = {
    var lines = Util.readFile(file).lines.toList
    var path = ("(" + QUOTE + "|[^/])+").r.findAllIn( query ).toList

    if(! path.isEmpty ) {
      val terminals = ("(" + QUOTE + "|[^,])+").r.findAllIn( path.last ).toList
      path = path.dropRight(1)
      
      val selections = for( terminal <- terminals ) yield {
        val span = ("(" + QUOTE + "|[^-])+").r.findAllIn( terminal )
        var (start, end) = follow( path ++ List(span.next), lines )
  
        for( p <- span )
          end += 1 + follow( List(p), lines.drop(end+1) )._2
        (lines.take(end+1).drop(start), start+1) // line numbers start from 1
      }
      selections.map( tup => (tup._1.mkString("\n"), tup._2) )
    } else
      List((lines.mkString("\n"), 0))
  }
}
