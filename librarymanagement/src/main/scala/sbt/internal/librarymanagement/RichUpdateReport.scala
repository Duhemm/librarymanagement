package sbt
package internal
package librarymanagement

import java.io.File
import sbt.librarymanagement._
import sbt.util.InterfaceUtil.{ m2o, t2 }
import scala.collection.JavaConverters._

/** Provides extra methods for filtering the contents of an `UpdateReport` and for obtaining references to a selected subset of the underlying files. */
final class RichUpdateReport(report: UpdateReport) {
  def recomputeStamps(): UpdateReport =
    {
      val files = report.cachedDescriptor +: allFiles
      val stamps = files.map(f => (f, f.lastModified.asInstanceOf[java.lang.Long])).toMap.asJava
      new UpdateReport(report.cachedDescriptor, report.configurations, report.stats, stamps)
    }

  import DependencyFilter._

  /** Obtains all successfully retrieved files in all configurations and modules. */
  def allFiles: Seq[File] = matching(DependencyFilter.allPass)

  /** Obtains all successfully retrieved files in configurations, modules, and artifacts matching the specified filter. */
  def matching(f: DependencyFilter): Seq[File] = select0(f).distinct

  /** Obtains all successfully retrieved files matching all provided filters.  An unspecified argument matches all files. */
  def select(configuration: ConfigurationFilter = configurationFilter(), module: ModuleFilter = moduleFilter(), artifact: ArtifactFilter = artifactFilter()): Seq[File] =
    matching(DependencyFilter.make(configuration, module, artifact))

  private[this] def select0(f: DependencyFilter): Seq[File] =
    for {
      cReport <- report.configurations
      mReport <- cReport.modules
      artFile <- mReport.artifacts
      if f(cReport.configuration, mReport.module, artFile.get1)
    } yield {
      if (artFile.get2 == null) sys.error("Null file: conf=" + cReport.configuration + ", module=" + mReport.module + ", art: " + artFile.get1)
      artFile.get2
    }

  /** Constructs a new report that only contains files matching the specified filter.*/
  private[sbt] def filter(f: DependencyFilter): UpdateReport =
    moduleReportMap { (configuration, modReport) =>
      modReport.withArtifacts(modReport.artifacts filter { a => f(configuration, modReport.module, a.get1) })
        .withMissingArtifacts(modReport.missingArtifacts filter { a => f(configuration, modReport.module, a) })
    }
  def substitute(f: (String, ModuleID, Seq[(Artifact, File)]) => Seq[(Artifact, File)]): UpdateReport =
    moduleReportMap { (configuration, modReport) =>
      val f2: (String, ModuleID, Array[xsbti.T2[Artifact, File]]) => Array[xsbti.T2[Artifact, File]] = {
        case (a1, a2, a3) =>
          val a32 = a3.toSeq map (artFile => (artFile.get1, artFile.get2))
          val newArtifacts = f(a1, a2, a32)
          val newArtifacts2 = newArtifacts.map { t2 }.toArray
          newArtifacts2
      }
      val newArtifacts = f2(configuration, modReport.module, modReport.artifacts)
      modReport.withArtifacts(newArtifacts)
      // modReport.copy(
      //   artifacts = newArtifacts,
      //   missingArtifacts = modReport.missingArtifacts
      // )
    }

  def toSeq: Seq[(String, ModuleID, Artifact, File)] =
    for {
      confReport <- report.configurations
      modReport <- confReport.modules
      artFile <- modReport.artifacts
      artifact = artFile.get1
      file = artFile.get2
    } yield (confReport.configuration, modReport.module, artifact, file)

  def allMissing: Seq[(String, ModuleID, Artifact)] =
    for (confReport <- report.configurations; modReport <- confReport.modules; artifact <- modReport.missingArtifacts) yield (confReport.configuration, modReport.module, artifact)

  def addMissing(f: ModuleID => Seq[Artifact]): UpdateReport =
    moduleReportMap { (configuration, modReport) =>
      modReport.withMissingArtifacts((modReport.missingArtifacts ++ f(modReport.module)).distinct)
      // modReport.copy(
      //   missingArtifacts = (modReport.missingArtifacts ++ f(modReport.module)).distinct
      // )
    }

  def moduleReportMap(f: (String, ModuleReport) => ModuleReport): UpdateReport =
    {
      val newConfigurations = report.configurations.map { confReport =>
        // import confReport._
        val newModules = confReport.modules map { modReport => f(confReport.configuration, modReport) }
        new ConfigurationReport(confReport.configuration, newModules, confReport.details)
      }
      new UpdateReport(report.cachedDescriptor, newConfigurations, report.stats, report.stamps)
    }

  // Directly copy-pasted from UpdateReport

  override def toString = "Update report:\n\t" + report.stats + "\n" + report.configurations.mkString

  /** All resolved modules in all configurations. */
  def allModules: Seq[ModuleID] =
    {
      val key = (m: ModuleID) => (m.organization, m.name, m.revision)
      report.configurations.flatMap(_.allModules).groupBy(key).toSeq map {
        case (k, v) =>
          v reduceLeft { (agg, x) =>
            agg.withConfigurations(
              (m2o(agg.configurations), m2o(x.configurations)) match {
                case (None, _)            => x.configurations
                case (Some(ac), None)     => xsbti.Maybe.just(ac)
                case (Some(ac), Some(xc)) => xsbti.Maybe.just(s"$ac;$xc")
              }
            )
          }
      }
    }

  def retrieve(f: (String, ModuleID, Artifact, File) => File): UpdateReport =
    new UpdateReport(report.cachedDescriptor, report.configurations map { _ retrieve f }, report.stats, report.stamps)

  /** Gets the report for the given configuration, or `None` if the configuration was not resolved.*/
  def configuration(s: String) = report.configurations.find(_.configuration == s)

  /** Gets the names of all resolved configurations.  This `UpdateReport` contains one `ConfigurationReport` for each configuration in this list. */
  def allConfigurations: Seq[String] = report.configurations.map(_.configuration)

  private[sbt] def withStats(us: UpdateStats): UpdateReport =
    new UpdateReport(
      report.cachedDescriptor,
      report.configurations,
      us,
      report.stamps
    )
}
