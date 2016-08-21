package sbt.internal.librarymanagement.formats

import _root_.sjsonnew.JsonFormat
import xsbti.GlobalLock

trait GlobalLockFormat { self: sjsonnew.BasicJsonProtocol =>

  implicit lazy val GlobalLockFormat: JsonFormat[GlobalLock] =
    project(MyCrazyReferences.referenced _, MyCrazyReferences.apply _)

}
