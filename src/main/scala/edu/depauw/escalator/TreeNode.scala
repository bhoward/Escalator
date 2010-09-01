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

import scala.xml._

/**
 * Represents a node in the source tree.
 * 
 * @author Brian Howard and Emily Bruckart
 */
trait TreeNode {
  /**
   * The File object corresponding to this node.  Barring race
   * conditions, we may assume this exists.
   */
  val file: File
  
  /**
   * @return the simple name of this node.
   */
  def name: String = file.getName
  
  /**
   * @return the output name for this node
   */
  def outputName: String
  
  /**
   * Generate the output representation of this node in target.
   * 
   * @param target The directory for the output
   * @param parent The DirNode containing this node; None at root
   * @param config The configuration object for this project
   */
  def generate(target: File, parent: Option[DirNode], config: Config): Unit
  
  /**
   * Create an HTML representation of this node of the tree.
   * 
   * @param path The (relative URL) path to this node
   * @param isRoot True if this is the top-level directory for the current page
   * @return an HTML element
   */
  def renderIndex(path: String, isRoot: Boolean): NodeSeq
}

/**
 * Companion object containing the TreeNode factory.
 */
object TreeNode {
  def apply(file: File): TreeNode = {
    if (file.isDirectory) {
      DirNode(file)
    } else {
      FileNode(file)
    }
  }
}

/**
 * A TreeNode corresponding to a source directory.
 * 
 * @param file The File object for this directory
 */
case class DirNode(file: File) extends TreeNode {
  // Collect information about this directory and subdirectories
  val children = for {
      child <- file.listFiles
      if (!child.getName.startsWith(".") // Ignore hidden files
          && child.getName != "esc-site") // and default output
    } yield TreeNode(child)
    
  def outputName = name + "/index.html"
  
  def generate(target: File, parent: Option[DirNode], config: Config) {
    val subTarget = if (parent.isDefined) {
      new File(target, name)
    } else {
      // At the root, just use the provided target.
      // This will typically be "esc-site" (instead of "src"...)
      target
    }
    
    if (!subTarget.exists) {
      subTarget.mkdir
    }
    
    for (child <- children) child.generate(subTarget, Some(this), config)
    
    if (!children.exists(_.outputName == "index.html")) {
      // Generate an index page
      val index = new File(subTarget, "index.html")
      val rp = Escalator.config.resourcepath 
      val mktreeHead = 
        <script type="text/javascript" src={rp + "/script/mktree.js"}></script> +:
        <link rel="stylesheet" href={rp + "/style/mktree.css"} type="text/css" />
      
      val title = "root" + file.toString.stripPrefix(config.source.toString)
      val body = NodeSeq fromSeq (
        <h1>{title}</h1> ++
        (if (parent.isDefined) {
          <div><a href="..">Parent Directory</a></div>
        } else Nil) ++
        renderIndex("", true)
      )
            
      Util.writeHTML(index, title, mktreeHead, body)
    }
  }
    
  def renderIndex(path: String, isRoot: Boolean): NodeSeq = 
    if (isRoot) {
      // DHTML collapsible tree from http://www.javascripttoolbox.com/lib/mktree/
      <ul class="mktree">
        { for (child <- children) yield child.renderIndex(path, false) }
      </ul>
    } else {
      // subdirectories start closed; add class="liOpen" to <li> to change this
      <li><a href={path + outputName}>{name}</a>
        <ul>
          { for (child <- children) yield child.renderIndex(path + name + "/", false) }
        </ul>
      </li>
    }
}

/**
 * A TreeNode corresponding to a single file.  Delegates most of its work to
 * a FileStrategy corresponding to the file type.
 */
case class FileNode(file: File) extends TreeNode {
  val strategy = FileStrategy(name)
  
  def outputName = strategy.outputName
  
  def generate(target: File, parent: Option[DirNode], config: Config) {
    for (gui <- Escalator.gui) gui.setStatus("Processing " + file)
    
    // parent should never be None here
    strategy.generate(this, target, parent.get, config)
  }
  
  def renderIndex(path: String, isRoot: Boolean): NodeSeq =
    strategy.renderIndex(path, name)
}
