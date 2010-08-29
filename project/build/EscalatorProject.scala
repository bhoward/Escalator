import sbt._

class EscalatorProject(info: ProjectInfo) extends DefaultProject(info) {
  val swing = "org.scala-lang" % "scala-swing" % "2.8.0"
  val compiler = "org.scala-lang" % "scala-compiler" % "2.8.0"
  val specs = "org.scala-tools.testing" %% "specs" % "1.6.5"
  val scalacheck = "org.scala-tools.testing" %% "scalacheck" % "1.7"
  val rhino = "rhino" % "js" % "1.7R2"

  override def compileClasspath = super.compileClasspath +++ extraCompileClasspath
  def extraCompileClasspath = path("lib_extra_compile") * "AppleJavaExtensions.jar"
  
  override def compileOptions = super.compileOptions ++ Seq(target(Target.Java1_5))
  
  override def mainClass = Some("edu.depauw.escalator.GUIMain")
  
  override def packagePaths = mainClasses // exclude mainResources from jar
  
  // the following is adapted from http://gracelessfailures.com/2009/11/24/build-package-zip-sbt.html
  def distPath = (
    // NOTE the double hashes (##) hoist the files in the preceeding directory
    // to the top level - putting them in the "base directory" in sbt's terminology
    ((outputPath ##) / defaultJarName) +++
    mainResources +++
    mainDependencies.scalaJars +++
    descendents(info.projectPath / "lib" ##, "*.jar") +++
    descendents(managedDependencyRootPath ** "compile" ##, "*.jar")
  )
  
  // creates a sane classpath including all JARs and populates the manifest with it
  override def manifestClassPath = Some(
    distPath.getFiles
    .filter(file => file.getName.endsWith(".jar") && file.getName != defaultJarName)
    .map(_.getName).mkString(" ") + " ."
  )
  
  def distName = "Escalator-%s.zip".format(version)
  
  lazy val zip = zipTask(distPath, "dist", distName) dependsOn (`package`) describedAs("Zips up the project.")
}
