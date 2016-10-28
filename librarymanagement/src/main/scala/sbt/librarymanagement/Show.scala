package sbt.librarymanagement

import sbt.internal.util.ShowLines

trait Show {

  implicit val ShowModuleID: ShowLines[ModuleID] =
    ShowLines { new RichModuleID(_).toString.lines.toSeq }

  implicit val ShowModuleReport: ShowLines[ModuleReport] =
    ShowLines { new RichModuleReport(_).toString.lines.toSeq }

}