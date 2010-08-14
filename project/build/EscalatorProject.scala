import sbt._

class EscalatorProject(info: ProjectInfo) extends DefaultProject(info) {
  val swing = "org.scala-lang" % "scala-swing" % "2.8.0"
  val compiler = "org.scala-lang" % "scala-compiler" % "2.8.0"
  val specs = "org.scala-tools.testing" %% "specs" % "1.6.5"
  val scalacheck = "org.scala-tools.testing" %% "scalacheck" % "1.7"
  val rhino = "rhino" % "js" % "1.7R2"
  
  override def compileOptions = super.compileOptions ++ Seq(target(Target.Java1_5))
  
  override def mainClass = Some("edu.depauw.escalator.Main")
}
