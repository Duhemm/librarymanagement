package sbt.internal.librarymanagement

import java.io.File
import org.apache.ivy.core
import core.module.descriptor.ModuleDescriptor
import sbt.util.Logger
import sbt.internal.util.CacheStore
import sbt.librarymanagement._
import sbt.librarymanagement.LibraryManagementCodec._
import JsonUtil._

private[sbt] object JsonUtil {
  def sbtOrgTemp = "org.scala-sbt.temp"
  def fakeCallerOrganization = "org.scala-sbt.temp-callers"
}

private[sbt] class JsonUtil(fileToStore: File => CacheStore) {

  def parseUpdateReport(md: ModuleDescriptor, path: File, cachedDescriptor: File, log: Logger): UpdateReport =
    {
      try {
        val reportStore = fileToStore(path)
        val lite = reportStore.read[UpdateReportLite]
        fromLite(lite, cachedDescriptor)
      } catch {
        case e: Throwable =>
          log.error("Unable to parse mini graph: " + path.getAbsolutePath.toString)
          throw e
      }
    }
  def writeUpdateReport(ur: UpdateReport, graphPath: File): Unit =
    {
      sbt.io.IO.createDirectory(graphPath.getParentFile)
      println("!" * 181)
      println("!" * 181)
      val graphStore = fileToStore(graphPath)
      val lite = toLite(ur)
      graphStore.write(lite)
      val read = fileToStore(graphPath).read[UpdateReportLite]
      if (lite == read) {
        println("#" * 181)
        println("Read and written reports are equal.")
        println("#" * 181)
      } else {
        println("#" * 181)
        println("Reports are different.")
        println("Lite:")
        println(lite.configurations()(0).details()(0).modules()(0))
        println("-" * 181)
        println("Read:")
        println(read.configurations()(0).details()(0).modules()(0))
        println("#" * 181)
      }
    }
  def toLite(ur: UpdateReport): UpdateReportLite =
    new UpdateReportLite(ur.configurations.toArray map { cr =>
      new ConfigurationReportLite(cr.configuration, cr.details.toArray map { oar =>
        new OrganizationArtifactReport(oar.organization, oar.name, oar.modules.toArray map { mr =>
          new ModuleReport(
            mr.module, mr.artifacts, mr.missingArtifacts, mr.status,
            mr.publicationDate, mr.resolver, mr.artifactResolver,
            mr.evicted, mr.evictedData, mr.evictedReason,
            mr.problem, mr.homepage, mr.extraAttributes,
            mr.isDefault, mr.branch, mr.configurations, mr.licenses,
            filterOutArtificialCallers(mr.callers)
          )
        })
      })
    })
  // #1763/#2030. Caller takes up 97% of space, so we need to shrink it down,
  // but there are semantics associated with some of them.
  def filterOutArtificialCallers(callers: Array[Caller]): Array[Caller] =
    if (callers.isEmpty) callers
    else {
      val nonArtificial = callers filter { c =>
        (c.caller.organization != sbtOrgTemp) &&
          (c.caller.organization != fakeCallerOrganization)
      }
      val interProj = (callers find { c =>
        c.caller.organization == sbtOrgTemp
      }).toArray
      interProj ++ nonArtificial
    }

  def fromLite(lite: UpdateReportLite, cachedDescriptor: File): UpdateReport =
    {
      val stats = new UpdateStats(0L, 0L, 0L, false)
      val configReports = lite.configurations map { cr =>
        val details = cr.details
        val modules = details flatMap {
          _.modules filter { mr =>
            !mr.evicted && mr.problem.isEmpty
          }
        }
        new ConfigurationReport(cr.configuration, modules, details)
      }
      new UpdateReport(cachedDescriptor, configReports, stats, Map.empty)
    }
}

// private[sbt] case class UpdateReportLite(configurations: Seq[ConfigurationReportLite])
// private[sbt] object UpdateReportLite {
//   implicit val pickler: Pickler[UpdateReportLite] with Unpickler[UpdateReportLite] = PicklerUnpickler.generate[UpdateReportLite]
// }

// private[sbt] case class ConfigurationReportLite(configuration: String, details: Seq[OrganizationArtifactReport])
