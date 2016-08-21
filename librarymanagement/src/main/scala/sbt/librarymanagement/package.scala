package sbt

import sbt.internal.util.ShowLines
import sbt.librarymanagement._

package object librarymanagement extends ResolversSyntax with Show {
  type ExclusionRule = InclExclRule
  type InclusionRule = InclExclRule

  implicit def richModuleID(moduleID: ModuleID): RichModuleID =
    new RichModuleID(moduleID)

  implicit def richModuleReport(moduleReport: ModuleReport): RichModuleReport =
    new RichModuleReport(moduleReport)

  implicit def richConfiguration(configuration: Configuration): RichConfiguration =
    new RichConfiguration(configuration)

  implicit def richArtifactFilterType(artifactTypeFilter: ArtifactTypeFilter): RichArtifactTypeFilter =
    new RichArtifactTypeFilter(artifactTypeFilter)
}

trait Show {

  implicit val ShowModuleID: ShowLines[ModuleID] =
    ShowLines { new RichModuleID(_).toString.lines.toSeq }

  implicit val ShowModuleReport: ShowLines[ModuleReport] =
    ShowLines { new RichModuleReport(_).toString.lines.toSeq }

}

// package sbt.internal.util

// trait ShowLines[A] {
//   def showLines(a: A): Seq[String]
// }
// object ShowLines {
//   def apply[A](f: A => Seq[String]): ShowLines[A] =
//     new ShowLines[A] {
//       def showLines(a: A): Seq[String] = f(a)
//     }

//   implicit class ShowLinesOp[A: ShowLines](a: A) {
//     def lines: Seq[String] = implicitly[ShowLines[A]].showLines(a)
//   }
// }
