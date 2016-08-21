import Dependencies._
import com.typesafe.tools.mima.core._, ProblemFilters._

// import scala.util.matching.Regex

import sbt.datatype.TpeRef
import sbt.datatype.CodecCodeGen

def baseVersion = "0.1.0"
def internalPath   = file("internal")

def commonSettings: Seq[Setting[_]] = Seq(
  scalaVersion := scala211,
  // publishArtifact in packageDoc := false,
  resolvers += Resolver.typesafeIvyRepo("releases"),
  resolvers += Resolver.sonatypeRepo("snapshots"),
  resolvers += Resolver.bintrayRepo("sbt", "maven-releases"),
  // concurrentRestrictions in Global += Util.testExclusiveRestriction,
  testOptions += Tests.Argument(TestFrameworks.ScalaCheck, "-w", "1"),
  javacOptions in compile ++= Seq("-target", "6", "-source", "6", "-Xlint", "-Xlint:-serial"),
  incOptions := incOptions.value.withNameHashing(true),
  crossScalaVersions := Seq(scala211),
  resolvers += Resolver.sonatypeRepo("public"),
  // scalacOptions += "-Ywarn-unused",
  // scalacOptions += "-Ywarn-unused-import",
  previousArtifact := None, // Some(organization.value %% moduleName.value % "1.0.0"),
  publishArtifact in Compile := true,
  publishArtifact in Test := false
)

lazy val root = (project in file(".")).
  aggregate(lm).
  settings(
    inThisBuild(Seq(
      homepage := Some(url("https://github.com/sbt/librarymanagement")),
      description := "Library management module for sbt",
      scmInfo := Some(ScmInfo(url("https://github.com/sbt/librarymanagement"), "git@github.com:sbt/librarymanagement.git")),
      bintrayPackage := "librarymanagement",
      git.baseVersion := baseVersion
    )),
    commonSettings,
    name := "LM Root",
    publish := {},
    publishLocal := {},
    publishArtifact in Compile := false,
    publishArtifact := false,
    customCommands
  )

def oneArg(tpe: TpeRef): TpeRef = {
  import scala.util.matching.Regex
  val pat = s"""${CodecCodeGen.removeTypeParameters(tpe.name)}<(.+?)>""".r
  val pat(arg0) = tpe.name
  TpeRef(arg0, false, false, false)
}

def twoArgs(tpe: TpeRef): List[TpeRef] = {
  import scala.util.matching.Regex
  val pat = s"""${CodecCodeGen.removeTypeParameters(tpe.name)}<(.+?), (.+?)>""".r
  val pat(arg0, arg1) = tpe.name
  TpeRef(arg0, false, false, false) :: TpeRef(arg1, false, false, false) :: Nil
}

lazy val myFormats: Map[String, TpeRef => List[String]] = Map(
  "xsbti.Maybe" ->
    { tpe => "sbt.internal.librarymanagement.formats.MaybeFormat" :: getFormats(oneArg(tpe)) },
  "Boolean" ->
    { tpe => "sbt.internal.librarymanagement.formats.MyBooleanFormat" :: Nil },
  "java.lang.Boolean" ->
    { tpe => "sbt.internal.librarymanagement.formats.MyBooleanFormat" :: Nil },
  "Integer" ->
    { tpe => "sbt.internal.librarymanagement.formats.MyIntegerFormat" :: Nil },
  "xsbti.T2" ->
    { tpe => "sbt.internal.librarymanagement.formats.T2Format" :: twoArgs(tpe).flatMap(getFormats) },
  "java.util.Map" ->
    { tpe => "sbt.internal.librarymanagement.formats.JavaMapFormat" :: twoArgs(tpe).flatMap(getFormats) },
  "java.util.Set" ->
    { tpe => "sbt.internal.librarymanagement.formats.JavaSetFormat" :: getFormats(oneArg(tpe)) },
  "sbt.librarymanagement.UpdateOptions" ->
    { tpe => "sbt.internal.librarymanagement.formats.UpdateOptionsFormat" :: Nil },
  "scala.xml.NodeSeq" ->
    { tpe => "sbt.internal.librarymanagement.formats.NodeSeqFormat" :: Nil },
  "xsbti.F1" ->
    { tpe => "sbt.internal.librarymanagement.formats.F1Format" :: twoArgs(tpe).flatMap(getFormats) },
  "xsbti.GlobalLock" ->
    { tpe => "sbt.internal.librarymanagement.formats.GlobalLockFormat" :: Nil },
  "xsbti.Logger" ->
    { tpe => "sbt.internal.librarymanagement.formats.LoggerFormat" :: Nil},
  "java.util.Date" ->
    { tpe => "sbt.internal.librarymanagement.formats.DateFormat" :: Nil },
  "org.apache.ivy.plugins.resolver.DependencyResolver" ->
    { tpe => "sbt.internal.librarymanagement.formats.DependencyResolverFormat" :: Nil }
)

lazy val primitives = Set("Boolean", "Integer")

lazy val excluded = Set(
  "sbt.librarymanagement.ArtifactTypeFilter",
  "sbt.librarymanagement.InclusionRule",
  "sbt.librarymanagement.ExclusionRule")

lazy val getFormats: TpeRef => List[String] =
  CodecCodeGen.extensibleFormatsForType {
    case TpeRef("sbt.internal.librarymanagement.RetrieveConfiguration", false, false, false) =>
      "sbt.librarymanagement.RetrieveConfigurationFormats" :: Nil
    case tpe @ TpeRef(name, _, _, _) if myFormats contains CodecCodeGen.removeTypeParameters(name) =>
      myFormats(CodecCodeGen.removeTypeParameters(name))(tpe)
    case TpeRef(name, _, _, _) if excluded contains CodecCodeGen.removeTypeParameters(name) =>
      Nil
    case TpeRef(name, false, false, false) if primitives contains name =>
      Nil
    case other =>
      CodecCodeGen.formatsForType(other)
  }

lazy val lm = (project in file("librarymanagement")).
  settings(
    commonSettings,
    name := "librarymanagement",
    libraryDependencies ++= Seq(
      utilLogging, sbtIO, utilTesting % Test,
      utilCollection, utilCompletion, utilCache, ivy, jsch, /*sbtSerialization,*/ scalaReflect.value, launcherInterface,
      sjsonnewScalaJson % Optional),
    libraryDependencies ++= scalaXml.value,
    resourceGenerators in Compile <+= (version, resourceManaged, streams, compile in Compile) map Util.generateVersionFile,
    binaryIssueFilters ++= Seq(),
    datatypeFormatsForType in generateDatatypes in Compile := getFormats
  ).
  enablePlugins(DatatypePlugin, JsonCodecPlugin)

def customCommands: Seq[Setting[_]] = Seq(
  commands += Command.command("release") { state =>
    // "clean" ::
    "so compile" ::
    "so publishSigned" ::
    "reload" ::
    state
  }
)
