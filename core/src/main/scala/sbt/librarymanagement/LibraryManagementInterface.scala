package sbt.librarymanagement

import java.io.File
import sbt.util.Logger

/**
 * Interface for library management
 */
abstract class LibraryManagementInterface {

  /**
   * Builds a ModuleDescriptor that describes a subproject with dependencies.
   *
   * @param moduleSetting It contains the information about the module including the dependencies.
   * @return A `ModuleDescriptor` describing a subproject and its dependencies.
   */
  def buildModule(moduleSetting: InlineConfiguration): ModuleDescriptor

  /**
   * Resolves the given module's dependencies performing a retrieval.
   *
   * @param module The module to be resolved.
   * @param configuration The update configuration.
   * @param uwconfig The configuration to handle unresolved warnings.
   * @param log The logger.
   * @return The result, either an unresolved warning or an update report. Note that this
   *         update report will or will not be successful depending on the `missingOk` option.
   */
  def update(module: ModuleDescriptor,
             configuration: UpdateConfiguration,
             uwconfig: UnresolvedWarningConfiguration,
             log: Logger): Either[UnresolvedWarning, UpdateReport]

  /**
   * Publishes the given module.
   *
   * @param module The module to be published.
   * @param configuration The publish configuration.
   * @param log The logger.
   */
  def publish(module: ModuleDescriptor, configuration: PublishConfiguration, log: Logger): Unit

  /**
   * Makes the `ivy.xml` file for the given module.
   *
   * @param module The module for which a `ivy.xml` file is to be created.
   * @param configuration The makeIvyFile configuration.
   * @param log The logger.
   * @return The `File` containing the Ivy description.
   */
  def makeIvyFile(module: ModuleDescriptor, configuration: DeliverConfiguration, log: Logger): File

  /**
   * Makes the `pom.xml` file for the given module.
   *
   * @param module The module for which a `.pom` file is to be created.
   * @param configuration The makePomFile configuration.
   * @param log The logger.
   * @return The `File` containing the POM descriptor.
   */
  def makePomFile(module: ModuleDescriptor, configuration: MakePomConfiguration, log: Logger): File
}

/**
 * Decribes the representation of a module, inclding its dependencies
 * and the version of Scala it uses, if any.
 */
trait ModuleDescriptor {

  /**
   * The direct dependencies of this module.
   */
  def directDependencies: Vector[ModuleID]

  /**
   * The information and module about the scala version that this module requires,
   * if any.
   */
  def scalaModuleInfo: Option[ScalaModuleInfo]
}
