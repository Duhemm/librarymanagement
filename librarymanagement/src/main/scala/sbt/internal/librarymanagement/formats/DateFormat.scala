package sbt.internal.librarymanagement.formats

import _root_.sjsonnew.JsonFormat
import java.util.Date
import java.text.SimpleDateFormat

trait DateFormat { self: sjsonnew.BasicJsonProtocol =>

  private val format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")

  implicit lazy val DateFormat: JsonFormat[Date] =
    project(_.toString, format.parse _)

}
