package sbt

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

  implicit def richConfigurationReport(configurationReport: ConfigurationReport): RichConfigurationReport =
    new RichConfigurationReport(configurationReport)
}
