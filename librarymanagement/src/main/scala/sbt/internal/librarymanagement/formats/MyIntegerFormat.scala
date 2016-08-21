package sbt.internal.librarymanagement.formats

import _root_.sjsonnew.JsonFormat

trait MyIntegerFormat { self: sjsonnew.BasicJsonProtocol =>

  implicit lazy val MyJavaIntegerFormat: JsonFormat[java.lang.Integer] =
    project(_.toString, (s: String) => java.lang.Integer.parseInt(s))

}
