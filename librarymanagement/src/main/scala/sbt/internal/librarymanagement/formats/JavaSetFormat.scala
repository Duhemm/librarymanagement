package sbt.internal.librarymanagement.formats

import _root_.sjsonnew.JsonFormat
import java.util.{ Set => JSet }
import scala.collection.JavaConverters._

trait JavaSetFormat { self: sjsonnew.BasicJsonProtocol =>

  implicit def JavaSetFormat[T: JsonFormat]: JsonFormat[JSet[T]] =
    project(_.asScala.toSet, (s: Set[T]) => s.asJava)

}
