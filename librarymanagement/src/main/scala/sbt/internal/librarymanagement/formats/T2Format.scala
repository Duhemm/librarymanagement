package sbt.internal.librarymanagement.formats

import _root_.sjsonnew.JsonFormat
import xsbti.T2
import sbt.util.InterfaceUtil.t2

trait T2Format { self: sjsonnew.BasicJsonProtocol =>

  implicit def T2Format[A1: JsonFormat, A2: JsonFormat]: JsonFormat[T2[A1, A2]] =
    project((t: T2[A1, A2]) => (t.get1, t.get2), t2 _)

}
