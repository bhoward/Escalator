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

import scala.xml.NodeSeq
import scala.io.Source

/**
 * Strategy to handle the filetype-dependent generation of
 * output files.
 * 
 * @author Brian Howard
 */
trait FileStrategy {
  /**
   * The name of the generated output file.
   */
  val outputName: String
  
  /**
   * Generate the output representation of this node in target.
   * 
   * @param source The FileNode for the input
   * @param target The directory for the output
   * @param parent The TreeNode containing this node
   * @param config The configuration object for this project
   */
  def generate(source: FileNode, target: File, parent: DirNode, config: Config): Unit
  
  /**
   * Generate one item of an index page for this node. Override for special cases.
   * 
   * @param path The (relative URL) path to this node
   * @param name The simple name of this node
   * @return The HTML content of the index list item
   */
  def renderIndex(path: String, name: String): NodeSeq =
    <li><a href={path + outputName}>{name}</a></li>
  
  // TODO in 2.8, can just concat XML elements with +:
  val SyntaxHighlighterHead = scala.xml.NodeSeq fromSeq (
    <script type="text/javascript" src="/resource/script/escalator.js"></script> ++
    <script type="text/javascript" src="/resource/jsMath/easy/load.js"></script> ++
    <script type="text/javascript" src="/resource/script/shCore.js"></script> ++
    <script type="text/javascript" src="/resource/script/shBrushJava.js"></script> ++
    <script type="text/javascript" src="/resource/script/shBrushScala.js"></script> ++
    <script type="text/javascript" src="/resource/script/shBrushPlain.js"></script> ++
    <script type="text/javascript" src="/resource/script/shBrushSpecs.js"></script> ++
    <link type="text/css" rel="stylesheet" href="/resource/style/shCore.css" /> ++
    <link type="text/css" rel="stylesheet" href="/resource/style/shThemeDefault.css" /> ++
    <script type="text/javascript">
      SyntaxHighlighter.config.clipboardSwf = '/resource/script/clipboard.swf';
      SyntaxHighlighter.all()
    </script>
  )

}

/**
 * Companion object containing the FileStrategy factory.
 */
object FileStrategy {
  def apply(name: String): FileStrategy = {
    val (base, ext) = Util.baseExtension(name)
    
    ext match {
      case "esc" => EscalatorStrategy(base + ".html", name + ".html")
      
      case "scala" => ScalaStrategy(name + ".html")
      
      case "java" => JavaStrategy(name + ".html")
      
      case "md" => MarkdownStrategy(base + ".html", name + ".html")
      
      case "class" => IgnoreStrategy
      
      case _ => DefaultStrategy(name)
    }
  }
}

case class EscalatorStrategy(outputName: String, rawName: String) extends FileStrategy {
  def generate(source: FileNode, target: File, parent: DirNode, config: Config) {
    val output = new File(target, outputName)
    val title = source.file.toString
    
    Escalator.config.documentPath = source.file // yeah, it's ugly...
    val bodyString = Util.markdown(Downalate(Util.readFile(source.file)))
    val body = scala.xml.Unparsed(bodyString)
    
    Util.writeHTML(output, title, SyntaxHighlighterHead, body)
    
    // Also generate the raw version of the file
    val rawOutput = new File(target, rawName)
    
    val rawBody = <pre class="brush: scala">{Source.fromFile(source.file).mkString}</pre>
    
    Util.writeHTML(rawOutput, title, SyntaxHighlighterHead, rawBody)
  }
  
  override def renderIndex(path: String, name: String): NodeSeq =
    <li><a href={path + outputName}>{name}</a> (<a href={path + rawName}>source</a>)</li>
}

case class MarkdownStrategy(outputName: String, rawName: String) extends FileStrategy {
  def generate(source: FileNode, target: File, parent: DirNode, config: Config) {
    val output = new File(target, outputName)
    val title = source.file.toString
    
    val mdString = Source.fromFile(source.file).mkString
    
    val bodyString = Util.markdown(mdString)
    val body = scala.xml.Unparsed(bodyString)
    
    Util.writeHTML(output, title, SyntaxHighlighterHead, body)
    
    // Also generate the raw version of the file
    val rawOutput = new File(target, rawName)
    
    val rawBody = <pre class="brush: scala">{mdString}</pre>
    
    Util.writeHTML(rawOutput, title, SyntaxHighlighterHead, rawBody)
  }
  
  override def renderIndex(path: String, name: String): NodeSeq =
    <li><a href={path + outputName}>{name}</a> (<a href={path + rawName}>source</a>)</li>
}

// TODO use Scala X-Ray instead
case class ScalaStrategy(outputName: String) extends FileStrategy {
  def generate(source: FileNode, target: File, parent: DirNode, config: Config) {
    val output = new File(target, outputName)
    val title = source.file.toString
        
    val body = <pre class="brush: scala">{Source.fromFile(source.file).mkString}</pre>
      
    Util.writeHTML(output, title, SyntaxHighlighterHead, body)
  }
}

case class JavaStrategy(outputName: String) extends FileStrategy {
  def generate(source: FileNode, target: File, parent: DirNode, config: Config) {
    val output = new File(target, outputName)
    val title = source.file.toString
        
    val body = <pre class="brush: java">{Source.fromFile(source.file).mkString}</pre>
      
    Util.writeHTML(output, title, SyntaxHighlighterHead, body)
  }
}

case object IgnoreStrategy extends FileStrategy {
  val outputName = ""
    
  def generate(source: FileNode, target: File, parent: DirNode, config: Config) {
    // Do nothing
  }
  
  override def renderIndex(path: String, name: String): NodeSeq =
    NodeSeq.Empty
}

case class DefaultStrategy(outputName: String) extends FileStrategy {
  def generate(source: FileNode, target: File, parent: DirNode, config: Config) {
    Util.copyFile(source.file, new File(target, outputName))
  }
}