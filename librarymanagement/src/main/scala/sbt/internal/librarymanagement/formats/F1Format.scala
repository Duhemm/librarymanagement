package sbt.internal.librarymanagement.formats

import _root_.sjsonnew.JsonFormat
import xsbti.F1

trait F1Format { self: sjsonnew.BasicJsonProtocol =>

  implicit def F1Format[T, U]: JsonFormat[F1[T, U]] =
    project(MyCrazyReferences.referenced _, MyCrazyReferences.apply _)

}
