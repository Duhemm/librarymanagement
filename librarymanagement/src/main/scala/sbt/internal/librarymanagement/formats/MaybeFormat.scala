package sbt.internal.librarymanagement.formats

import _root_.sjsonnew.JsonFormat
import xsbti.Maybe
import sbt.util.InterfaceUtil.{ m2o, o2m }

trait MaybeFormat { self: sjsonnew.BasicJsonProtocol =>

  implicit def MaybeFormat[T: JsonFormat]: JsonFormat[Maybe[T]] =
    project(
      (in: Maybe[T]) => if (in.isDefined) Option(in.get) else None,
      (in: Option[T]) => if (in == null) ??? else o2m(in)
    )

}
