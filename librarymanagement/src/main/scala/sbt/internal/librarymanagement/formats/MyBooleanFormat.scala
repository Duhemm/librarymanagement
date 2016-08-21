package sbt.internal.librarymanagement.formats

import _root_.sjsonnew.JsonFormat

trait MyBooleanFormat { self: sjsonnew.BasicJsonProtocol =>

  implicit lazy val MyJavaBooleanFormat: JsonFormat[java.lang.Boolean] =
    project(_.toString, java.lang.Boolean.parseBoolean _)

}
