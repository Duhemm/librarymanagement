package sbt.internal.librarymanagement.formats

import _root_.sjsonnew.JsonFormat

trait MyLongFormat { self: sjsonnew.BasicJsonProtocol =>

  implicit lazy val MyJavaLongFormat: JsonFormat[java.lang.Long] =
    project(_.toString, (s: String) => java.lang.Long.parseLong(s))

}
