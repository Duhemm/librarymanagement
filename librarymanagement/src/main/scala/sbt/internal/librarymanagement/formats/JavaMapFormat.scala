package sbt.internal.librarymanagement.formats

import _root_.sjsonnew.JsonFormat
import java.util.{ Map => JMap }
import scala.collection.JavaConverters._

trait JavaMapFormat { self: sjsonnew.BasicJsonProtocol =>

  implicit def JavaMapFormat[K: JsonFormat, V: JsonFormat]: JsonFormat[JMap[K, V]] =
    project(_.asScala.toMap, (m: Map[K, V]) => m.asJava)

}
