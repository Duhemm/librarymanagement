import sbt._
import Keys._

object Dependencies {
  lazy val scala211 = "2.11.8"

  val ioVersion = "1.0.0-M6"
  val utilVersion = "0.1.0-M13-64a648656cd48ff2d5a1e078df1eb2ebf55cd4dd"
  lazy val sbtIO = "org.scala-sbt" %% "io" % ioVersion
  lazy val utilCollection = "org.scala-sbt" %% "util-collection" % utilVersion
  lazy val utilLogging = "org.scala-sbt" %% "util-logging" % utilVersion
  lazy val utilTesting = "org.scala-sbt" %% "util-testing" % utilVersion
  lazy val utilCompletion = "org.scala-sbt" %% "util-completion" % utilVersion
  lazy val utilCache = "org.scala-sbt" %% "util-cache" % utilVersion

  lazy val launcherInterface = "org.scala-sbt" % "launcher-interface" % "1.0.0"
  lazy val ivy = "org.scala-sbt.ivy" % "ivy" % "2.3.0-sbt-2cc8d2761242b072cedb0a04cb39435c4fa24f9a"
  lazy val jsch = "com.jcraft" % "jsch" % "0.1.46" intransitive ()
  // lazy val sbtSerialization = "org.scala-sbt" %% "serialization" % "0.1.2"
  lazy val scalaReflect = Def.setting { "org.scala-lang" % "scala-reflect" % scalaVersion.value }
  lazy val scalaXml = scala211Module("scala-xml", "1.0.5")
  lazy val sjsonnewVersion = "0.4.1"
  lazy val sjsonnewScalaJson = "com.eed3si9n" %% "sjson-new-scalajson" % sjsonnewVersion

  private def scala211Module(name: String, moduleVersion: String) =
    Def.setting {
      scalaVersion.value match {
        case sv if (sv startsWith "2.9.") || (sv startsWith "2.10.") => Nil
        case _ => ("org.scala-lang.modules" %% name % moduleVersion) :: Nil
      }
    }
}
