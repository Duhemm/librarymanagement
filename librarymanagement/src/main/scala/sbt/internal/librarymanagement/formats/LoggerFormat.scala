package sbt.internal.librarymanagement.formats

import _root_.sjsonnew.JsonFormat
import xsbti.Logger

trait LoggerFormat { self: sjsonnew.BasicJsonProtocol =>

  implicit lazy val LoggerFormat: JsonFormat[Logger] =
    project(MyCrazyReferences.referenced _, MyCrazyReferences.apply _)

}
