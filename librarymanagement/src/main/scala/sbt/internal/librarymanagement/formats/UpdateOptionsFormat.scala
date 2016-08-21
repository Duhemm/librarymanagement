package sbt.internal.librarymanagement
package formats

import _root_.sjsonnew.JsonFormat
import sbt.librarymanagement.{ CircularDependencyLevel, UpdateOptions }

trait UpdateOptionsFormat { self: sjsonnew.BasicJsonProtocol =>

  implicit lazy val UpdateOptionsFormat: JsonFormat[UpdateOptions] =
    project(
      (uo: UpdateOptions) => {
        (
          uo.circularDependencyLevel.name,
          uo.interProjectFirst,
          uo.latestSnapshots,
          uo.consolidatedResolution,
          uo.cachedResolution
        )
      },
      (xs: (String, Boolean, Boolean, Boolean, Boolean)) => {
        new UpdateOptions(levels(xs._1), xs._2, xs._3, xs._4, xs._5, ConvertResolver.defaultConvert)
      }
    )

  private val levels: Map[String, CircularDependencyLevel] = Map(
    "warn" -> CircularDependencyLevel.Warn,
    "ignore" -> CircularDependencyLevel.Ignore,
    "error" -> CircularDependencyLevel.Error
  )

}
