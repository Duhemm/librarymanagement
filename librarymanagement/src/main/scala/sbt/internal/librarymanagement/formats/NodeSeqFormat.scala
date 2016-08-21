package sbt.internal.librarymanagement.formats

import _root_.sjsonnew.JsonFormat
import scala.xml.NodeSeq

trait NodeSeqFormat { self: sjsonnew.BasicJsonProtocol =>

  implicit lazy val NodeSeqFormat: JsonFormat[NodeSeq] =
    project(MyCrazyReferences.referenced _, MyCrazyReferences.apply _)

}
