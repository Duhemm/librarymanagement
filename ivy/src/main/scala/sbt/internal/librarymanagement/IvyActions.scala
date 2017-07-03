/* sbt -- Simple Build Tool
 * Copyright 2008, 2009, 2010 Mark Harrah
 */
package sbt.internal.librarymanagement

import java.io.File
import scala.xml.{ Node => XNode, NodeSeq }
import ivyint.CachedResolutionResolveEngine

import org.apache.ivy.Ivy
import org.apache.ivy.core.{ IvyPatternHelper, LogOptions }
import org.apache.ivy.core.deliver.DeliverOptions
import org.apache.ivy.core.install.InstallOptions
import org.apache.ivy.core.module.descriptor.{
  Artifact => IArtifact,
  MDArtifact,
  ModuleDescriptor,
  DefaultModuleDescriptor
}
import org.apache.ivy.core.resolve.ResolveOptions
import org.apache.ivy.plugins.resolver.{ BasicResolver, DependencyResolver }
import org.apache.ivy.util.filter.{ Filter => IvyFilter }
import sbt.io.{ IO, PathFinder }
import sbt.util.Logger
import sbt.librarymanagement._, syntax._
import InternalDefaults._

final class DeliverConfiguration(
    val deliverIvyPattern: String,
    val status: String,
    val configurations: Option[Vector[Configuration]],
    val logging: UpdateLogging
)

final case class MakePomConfiguration(
    file: File,
    moduleInfo: ModuleInfo,
    configurations: Option[Vector[Configuration]] = None,
    extra: NodeSeq = NodeSeq.Empty,
    process: XNode => XNode = n => n,
    filterRepositories: MavenRepository => Boolean = _ => true,
    allRepositories: Boolean,
    includeTypes: Set[String] = Set(Artifact.DefaultType, Artifact.PomType)
)

/** @param exclude is a map from ModuleID to classifiers that were previously tried and failed, so should now be excluded */
final case class GetClassifiersConfiguration(
    module: GetClassifiersModule,
    exclude: Map[ModuleID, Set[String]],
    configuration: UpdateConfiguration,
    scalaModuleInfo: Option[ScalaModuleInfo],
    sourceArtifactTypes: Set[String],
    docArtifactTypes: Set[String]
)
final case class GetClassifiersModule(
    id: ModuleID,
    modules: Vector[ModuleID],
    configurations: Vector[Configuration],
    classifiers: Vector[String]
)

object IvyActions {

  /** Installs the dependencies of the given 'module' from the resolver named 'from' to the resolver named 'to'.*/
  def install(module: IvySbt#Module, from: String, to: String, log: Logger): Unit = {
    module.withModule(log) { (ivy, md, default) =>
      for (dependency <- md.getDependencies) {
        log.info("Installing " + dependency)
        val options = new InstallOptions
        options.setValidate(module.moduleSettings.validate)
        options.setTransitive(dependency.isTransitive)
        ivy.install(dependency.getDependencyRevisionId, from, to, options)
      }
    }
  }

  /** Clears the Ivy cache, as configured by 'config'. */
  def cleanCache(ivy: IvySbt, log: Logger) = ivy.withIvy(log) { iv =>
    iv.getSettings.getResolutionCacheManager.clean()
    iv.getSettings.getRepositoryCacheManagers.foreach(_.clean())
  }

  /**
   * Cleans the cached resolution cache, if any.
   * This is called by clean.
   */
  private[sbt] def cleanCachedResolutionCache(module: IvySbt#Module, log: Logger): Unit =
    module.withModule(log) { (ivy, md, default) =>
      module.owner.cleanCachedResolutionCache(md, log)
    }

  /** Creates a Maven pom from the given Ivy configuration*/
  def makePom(module: IvySbt#Module, configuration: MakePomConfiguration, log: Logger): Unit = {
    import configuration.{
      allRepositories,
      moduleInfo,
      configurations,
      extra,
      file,
      filterRepositories,
      process,
      includeTypes
    }
    module.withModule(log) { (ivy, md, default) =>
      (new MakePom(log)).write(
        ivy,
        md,
        moduleInfo,
        configurations,
        includeTypes,
        extra,
        process,
        filterRepositories,
        allRepositories,
        file
      )
      log.info("Wrote " + file.getAbsolutePath)
    }
  }

  def deliver(module: IvySbt#Module, configuration: DeliverConfiguration, log: Logger): File = {
    import configuration._
    module.withModule(log) {
      case (ivy, md, _) =>
        val revID = md.getModuleRevisionId
        val options = DeliverOptions.newInstance(ivy.getSettings).setStatus(status)
        options.setConfs(IvySbt.getConfigurations(md, configurations))
        ivy.deliver(revID, revID.getRevision, deliverIvyPattern, options)
        deliveredFile(ivy, deliverIvyPattern, md)
    }
  }
  def deliveredFile(ivy: Ivy, pattern: String, md: ModuleDescriptor): File =
    ivy.getSettings.resolveFile(
      IvyPatternHelper.substitute(pattern, md.getResolvedModuleRevisionId)
    )

  def publish(module: IvySbt#Module, configuration: PublishConfiguration, log: Logger): Unit = {
    val resolverName = configuration.resolverName match {
      case Some(x) => x
      case _       => sys.error("Resolver name is not specified")
    }
    val ivyFile = configuration.metadataFile
    val artifacts = Map(configuration.artifacts: _*)
    val checksums = getChecksums(configuration.checksums)
    val overwrite = getPublishOverwrite(configuration.overwrite)
    module.withModule(log) {
      case (ivy, md, _) =>
        val resolver = ivy.getSettings.getResolver(resolverName)
        if (resolver eq null) sys.error("Undefined resolver '" + resolverName + "'")
        val ivyArtifact = ivyFile map { file =>
          (MDArtifact.newIvyArtifact(md), file)
        }
        val cross = crossVersionMap(module.moduleSettings)
        val as = mapArtifacts(md, cross, artifacts) ++ ivyArtifact.toList
        withChecksums(resolver, checksums) { publish(md, as, resolver, overwrite = overwrite) }
    }
  }
  private[this] def withChecksums[T](resolver: DependencyResolver, checksums: Vector[String])(
      act: => T
  ): T =
    resolver match { case br: BasicResolver => withChecksums(br, checksums)(act); case _ => act }
  private[this] def withChecksums[T](resolver: BasicResolver, checksums: Vector[String])(
      act: => T
  ): T = {
    val previous = resolver.getChecksumAlgorithms
    resolver.setChecksums(checksums mkString ",")
    try { act } finally { resolver.setChecksums(previous mkString ",") }
  }
  private def crossVersionMap(moduleSettings: ModuleSettings): Option[String => String] =
    moduleSettings match {
      case i: InlineConfiguration => CrossVersion(i.module, i.scalaModuleInfo)
      case _                      => None
    }
  def mapArtifacts(
      module: ModuleDescriptor,
      cross: Option[String => String],
      artifacts: Map[Artifact, File]
  ): Vector[(IArtifact, File)] = {
    val rawa = artifacts.keys.toVector
    val seqa = CrossVersion.substituteCross(rawa, cross)
    val zipped = rawa zip IvySbt.mapArtifacts(module, seqa)
    zipped map { case (a, ivyA) => (ivyA, artifacts(a)) }
  }

  /**
   * Updates one module's dependencies performing a dependency resolution and retrieval.
   *
   * The following mechanism uses ivy under the hood.
   *
   * @param module The module to be resolved.
   * @param configuration The update configuration.
   * @param uwconfig The configuration to handle unresolved warnings.
   * @param log The logger.
   * @return The result, either an unresolved warning or an update report. Note that this
   *         update report will or will not be successful depending on the `missingOk` option.
   */
  private[sbt] def updateEither(
      module: IvySbt#Module,
      configuration: UpdateConfiguration,
      uwconfig: UnresolvedWarningConfiguration,
      log: Logger
  ): Either[UnresolvedWarning, UpdateReport] = {
    module.withModule(log) {
      case (ivy, moduleDescriptor, defaultConf) =>
        // Warn about duplicated and inconsistent dependencies
        val iw = IvySbt.inconsistentDuplicateWarning(moduleDescriptor)
        iw.foreach(log.warn(_))

        val logicalClock = getLogicalClock(configuration.logicalClock)
        val metadataDirectory = configuration.metadataDirectory

        // Create inputs, resolve and retrieve the module descriptor
        val inputs = ResolutionInputs(ivy, moduleDescriptor, configuration, log)
        val resolutionResult: Either[ResolveException, UpdateReport] = {
          if (module.owner.configuration.updateOptions.cachedResolution && metadataDirectory.isDefined) {
            val cache =
              metadataDirectory.getOrElse(sys.error("Missing directory for cached resolution."))
            cachedResolveAndRetrieve(inputs, logicalClock, cache)
          } else resolveAndRetrieve(inputs, defaultConf)
        }

        // Convert to unresolved warning or retrieve update report
        resolutionResult.fold(
          exception => Left(UnresolvedWarning(exception, uwconfig)),
          ur0 => {
            val ur = configuration.retrieveManaged match {
              case Some(retrieveConf) => retrieve(log, ivy, ur0, retrieveConf)
              case _                  => ur0
            }
            Right(ur)
          }
        )
    }
  }

  def groupedConflicts[T](moduleFilter: ModuleFilter, grouping: ModuleID => T)(
      report: UpdateReport
  ): Map[T, Set[String]] =
    report.configurations.flatMap { confReport =>
      val evicted = confReport.evicted.filter(moduleFilter)
      val evictedSet = evicted.map(m => (m.organization, m.name)).toSet
      val conflicted =
        confReport.allModules.filter(mod => evictedSet((mod.organization, mod.name)))
      grouped(grouping)(conflicted ++ evicted)
    }.toMap

  def grouped[T](grouping: ModuleID => T)(mods: Seq[ModuleID]): Map[T, Set[String]] =
    mods groupBy (grouping) mapValues (_.map(_.revision).toSet)

  private[sbt] def transitiveScratch(
      ivySbt: IvySbt,
      label: String,
      config: GetClassifiersConfiguration,
      uwconfig: UnresolvedWarningConfiguration,
      log: Logger
  ): UpdateReport = {
    import config.{ configuration => c, scalaModuleInfo, module => mod }
    import mod.{ id, modules => deps }
    val base = restrictedCopy(id, true).withName(id.name + "$" + label)
    val module =
      new ivySbt.Module(
        InlineConfiguration(false, scalaModuleInfo, base, ModuleInfo(base.name), deps))
    val report = updateEither(module, c, uwconfig, log) match {
      case Right(r) => r
      case Left(w) =>
        throw w.resolveException
    }
    val newConfig = config.copy(module = mod.copy(modules = report.allModules))
    updateClassifiers(ivySbt, newConfig, uwconfig, Vector(), log)
  }

  /**
   * Creates explicit artifacts for each classifier in `config.module`, and then attempts to resolve them directly. This
   * is for Maven compatibility, where these artifacts are not "published" in the POM, so they don't end up in the Ivy
   * that sbt generates for them either.<br>
   * Artifacts can be obtained from calling toSeq on UpdateReport.<br>
   * In addition, retrieves specific Ivy artifacts if they have one of the requested `config.configuration.types`.
   * @param config important to set `config.configuration.types` to only allow artifact types that can correspond to
   *               "classified" artifacts (sources and javadocs).
   */
  private[sbt] def updateClassifiers(
      ivySbt: IvySbt,
      config: GetClassifiersConfiguration,
      uwconfig: UnresolvedWarningConfiguration,
      artifacts: Vector[(String, ModuleID, Artifact, File)],
      log: Logger
  ): UpdateReport = {
    import config.{ configuration => c, module => mod, _ }
    import mod.{ configurations => confs, _ }
    val artifactFilter = getArtifactTypeFilter(c.artifactFilter)
    assert(classifiers.nonEmpty, "classifiers cannot be empty")
    assert(artifactFilter.types.nonEmpty, "UpdateConfiguration must filter on some types")
    val baseModules = modules map { m =>
      restrictedCopy(m, true)
    }
    // Adding list of explicit artifacts here.
    val deps = baseModules.distinct flatMap classifiedArtifacts(classifiers, exclude, artifacts)
    val base = restrictedCopy(id, true).withName(id.name + classifiers.mkString("$", "_", ""))
    val module = new ivySbt.Module(
      InlineConfiguration(false, scalaModuleInfo, base, ModuleInfo(base.name), deps)
        .withConfigurations(confs)
    )
    // c.copy ensures c.types is preserved too
    val upConf = c.withMissingOk(true)
    updateEither(module, upConf, uwconfig, log) match {
      case Right(r) =>
        // The artifacts that came from Ivy don't have their classifier set, let's set it according to
        // FIXME: this is only done because IDE plugins depend on `classifier` to determine type. They
        val typeClassifierMap: Map[String, String] =
          ((sourceArtifactTypes.toIterable map (_ -> Artifact.SourceClassifier))
            :: (docArtifactTypes.toIterable map (_ -> Artifact.DocClassifier)) :: Nil).flatten.toMap
        r.substitute { (conf, mid, artFileSeq) =>
          artFileSeq map {
            case (art, f) =>
              // Deduce the classifier from the type if no classifier is present already
              art.withClassifier(art.classifier orElse typeClassifierMap.get(art.`type`)) -> f
          }
        }
      case Left(w) =>
        throw w.resolveException
    }
  }
  // This version adds explicit artifact
  private[sbt] def classifiedArtifacts(
      classifiers: Vector[String],
      exclude: Map[ModuleID, Set[String]],
      artifacts: Vector[(String, ModuleID, Artifact, File)]
  )(m: ModuleID): Option[ModuleID] = {
    def sameModule(m1: ModuleID, m2: ModuleID): Boolean =
      m1.organization == m2.organization && m1.name == m2.name && m1.revision == m2.revision
    def explicitArtifacts = {
      val arts = (artifacts collect {
        case (_, x, art, _) if sameModule(m, x) && art.classifier.isDefined => art
      }).distinct
      if (arts.isEmpty) None
      else Some(intransitiveModuleWithExplicitArts(m, arts))
    }
    def hardcodedArtifacts = classifiedArtifacts(classifiers, exclude)(m)
    explicitArtifacts orElse hardcodedArtifacts
  }
  private def classifiedArtifacts(
      classifiers: Vector[String],
      exclude: Map[ModuleID, Set[String]]
  )(m: ModuleID): Option[ModuleID] = {
    val excluded = exclude getOrElse (restrictedCopy(m, false), Set.empty)
    val included = classifiers filterNot excluded
    if (included.isEmpty) None
    else {
      Some(
        intransitiveModuleWithExplicitArts(
          module = m,
          arts = classifiedArtifacts(m.name, included)
        )
      )
    }
  }

  /**
   * Explicitly set an "include all" rule (the default) because otherwise, if we declare ANY explicitArtifacts,
   * [[org.apache.ivy.core.resolve.IvyNode#getArtifacts]] (in Ivy 2.3.0-rc1) will not merge in the descriptor's
   * artifacts and will only keep the explicitArtifacts.
   * <br>
   * Look for the comment saying {{{
   *   // and now we filter according to include rules
   * }}}
   * in `IvyNode`, which iterates on `includes`, which will ordinarily be empty because higher up, in {{{
   *   addAllIfNotNull(includes, usage.getDependencyIncludesSet(rootModuleConf));
   * }}}
   * `usage.getDependencyIncludesSet` returns null if there are no (explicit) include rules.
   */
  private def intransitiveModuleWithExplicitArts(
      module: ModuleID,
      arts: Vector[Artifact]
  ): ModuleID =
    module
      .withIsTransitive(false)
      .withExplicitArtifacts(arts)
      .withInclusions(Vector(InclExclRule.everything))

  def addExcluded(
      report: UpdateReport,
      classifiers: Vector[String],
      exclude: Map[ModuleID, Set[String]]
  ): UpdateReport =
    report.addMissing { id =>
      classifiedArtifacts(id.name, classifiers filter getExcluded(id, exclude))
    }
  def classifiedArtifacts(name: String, classifiers: Vector[String]): Vector[Artifact] =
    classifiers map { c =>
      Artifact.classified(name, c)
    }
  private[this] def getExcluded(id: ModuleID, exclude: Map[ModuleID, Set[String]]): Set[String] =
    exclude.getOrElse(restrictedCopy(id, false), Set.empty[String])

  def extractExcludes(report: UpdateReport): Map[ModuleID, Set[String]] =
    report.allMissing flatMap {
      case (_, mod, art) =>
        art.classifier.map { c =>
          (restrictedCopy(mod, false), c)
        }
    } groupBy (_._1) map { case (mod, pairs) => (mod, pairs.map(_._2).toSet) }

  private[this] def restrictedCopy(m: ModuleID, confs: Boolean) =
    ModuleID(m.organization, m.name, m.revision)
      .withCrossVersion(m.crossVersion)
      .withExtraAttributes(m.extraAttributes)
      .withConfigurations(if (confs) m.configurations else None)
      .branch(m.branchName)

  /**
   * Represents the inputs to pass in to [[resolveAndRetrieve]] and [[cachedResolveAndRetrieve]].
   *
   * @param ivy The ivy instance to resolve and retrieve dependencies.
   * @param module The module descriptor to be resolved.
   * @param updateConfiguration The update configuration for [[ResolveOptions]].
   * @param log The logger.
   */
  private case class ResolutionInputs(
      ivy: Ivy,
      module: DefaultModuleDescriptor,
      updateConfiguration: UpdateConfiguration,
      log: Logger
  )

  implicit def toIvyFilter(f: ArtifactTypeFilter): IvyFilter = new IvyFilter {
    override def accept(o: Object): Boolean = Option(o) exists {
      case a: IArtifact => applyFilter(f, a)
    }

    def applyFilter(f: ArtifactTypeFilter, a: IArtifact): Boolean =
      (f.types contains a.getType) ^ f.inverted
  }

  /**
   * Defines the internal entrypoint of module resolution and retrieval.
   *
   * This method is the responsible of populating [[ResolveOptions]] and pass
   * it in to the ivy instance to perform the module resolution.
   *
   * It returns an already resolved [[UpdateReport]] instead of a [[ResolveReport]]
   * like its counterpart [[CachedResolutionResolveEngine.customResolve]].
   *
   * @param inputs The resolution inputs.
   * @param defaultModuleConfiguration The default ivy configuration.
   * @return The result of the resolution.
   */
  private[this] def resolveAndRetrieve(
      inputs: ResolutionInputs,
      defaultModuleConfiguration: String
  ): Either[ResolveException, UpdateReport] = {
    // Populate resolve options from the passed arguments
    val ivyInstance = inputs.ivy
    val moduleDescriptor = inputs.module
    val updateConfiguration = inputs.updateConfiguration
    val logging = getUpdateLogging(updateConfiguration.logging)
    val resolveOptions = new ResolveOptions
    val resolveId = ResolveOptions.getDefaultResolveId(moduleDescriptor)
    val artifactFilter = getArtifactTypeFilter(updateConfiguration.artifactFilter)
    val offline = getOffline(updateConfiguration.offline)
    val frozen = getFrozen(updateConfiguration.frozen)
    val missingOk = getMissingOk(updateConfiguration.missingOk)
    resolveOptions.setResolveId(resolveId)
    resolveOptions.setArtifactFilter(artifactFilter)
    resolveOptions.setUseCacheOnly(offline)
    resolveOptions.setLog(ivyLogLevel(logging))
    if (frozen) {
      resolveOptions.setTransitive(false)
      resolveOptions.setCheckIfChanged(false)
    }
    ResolutionCache.cleanModule(
      moduleDescriptor.getModuleRevisionId,
      resolveId,
      ivyInstance.getSettings.getResolutionCacheManager
    )

    val resolveReport = ivyInstance.resolve(moduleDescriptor, resolveOptions)
    if (resolveReport.hasError && !missingOk) {
      // If strict error, collect report information and generated UnresolvedWarning
      val messages = resolveReport.getAllProblemMessages.toArray.map(_.toString).distinct
      val failedPaths = resolveReport.getUnresolvedDependencies.map { node =>
        val moduleID = IvyRetrieve.toModuleID(node.getId)
        val path = IvyRetrieve
          .findPath(node, moduleDescriptor.getModuleRevisionId)
          .map(x => IvyRetrieve.toModuleID(x.getId))
        moduleID -> path
      }.toMap
      val failedModules = failedPaths.keys.toSeq
      Left(new ResolveException(messages, failedModules, failedPaths))
    } else {
      // If no strict error, we convert the resolve report into an update report
      val cachedDescriptor = ivyInstance.getSettings.getResolutionCacheManager
        .getResolvedIvyFileInCache(moduleDescriptor.getModuleRevisionId)
      Right(IvyRetrieve.updateReport(resolveReport, cachedDescriptor))
    }
  }

  /**
   * Resolves and retrieves a module with a cache mechanism defined
   * <a href="http://www.scala-sbt.org/0.13/docs/Cached-Resolution.html">here</a>.
   *
   * It's the cached version of [[resolveAndRetrieve]].
   *
   * @param inputs The resolution inputs.
   * @param logicalClock The clock to check if a file is outdated or not.
   * @param cache The optional cache dependency.
   * @return The result of the cached resolution.
   */
  private[this] def cachedResolveAndRetrieve(
      inputs: ResolutionInputs,
      logicalClock: LogicalClock,
      cache: File
  ): Either[ResolveException, UpdateReport] = {
    val log = inputs.log
    val descriptor = inputs.module
    val updateConfiguration = inputs.updateConfiguration
    val logging = getUpdateLogging(updateConfiguration.logging)
    val resolver = inputs.ivy.getResolveEngine.asInstanceOf[CachedResolutionResolveEngine]
    val resolveOptions = new ResolveOptions
    val resolveId = ResolveOptions.getDefaultResolveId(descriptor)
    val artifactFilter = getArtifactTypeFilter(updateConfiguration.artifactFilter)
    val offline = getOffline(updateConfiguration.offline)
    val frozen = getFrozen(updateConfiguration.frozen)
    val missingOk = getMissingOk(updateConfiguration.missingOk)
    resolveOptions.setResolveId(resolveId)
    resolveOptions.setArtifactFilter(artifactFilter)
    resolveOptions.setUseCacheOnly(offline)
    resolveOptions.setLog(ivyLogLevel(logging))
    if (frozen) {
      resolveOptions.setTransitive(false)
      resolveOptions.setCheckIfChanged(false)
    }
    resolver.customResolve(descriptor, missingOk, logicalClock, resolveOptions, cache, log)
  }

  private def retrieve(
      log: Logger,
      ivy: Ivy,
      report: UpdateReport,
      config: RetrieveConfiguration
  ): UpdateReport = {
    val copyChecksums = ivy.getVariable(ConvertResolver.ManagedChecksums).toBoolean
    val base = getRetrieveDirectory(config.retrieveDirectory)
    val pattern = getRetrievePattern(config.outputPattern)
    val configurationNames = config.configurationsToRetrieve map { _.toSet }
    val existingFiles = PathFinder(base).allPaths.get filterNot { _.isDirectory }
    val toCopy = new collection.mutable.HashSet[(File, File)]
    val retReport = report retrieve { (conf, mid, art, cached) =>
      configurationNames match {
        case None => performRetrieve(conf, mid, art, base, pattern, cached, copyChecksums, toCopy)
        case Some(names) if names(conf) =>
          performRetrieve(conf, mid, art, base, pattern, cached, copyChecksums, toCopy)
        case _ => cached
      }
    }
    IO.copy(toCopy)
    val resolvedFiles = toCopy.map(_._2)
    val sync = getSync(config.sync)
    if (sync) {
      val filesToDelete = existingFiles.filterNot(resolvedFiles.contains)
      filesToDelete foreach { f =>
        log.info(s"Deleting old dependency: ${f.getAbsolutePath}")
        f.delete()
      }
    }

    retReport
  }

  private def performRetrieve(
      conf: String,
      mid: ModuleID,
      art: Artifact,
      base: File,
      pattern: String,
      cached: File,
      copyChecksums: Boolean,
      toCopy: collection.mutable.HashSet[(File, File)]
  ): File = {
    val to = retrieveTarget(conf, mid, art, base, pattern)
    toCopy += ((cached, to))

    if (copyChecksums) {
      // Copy over to the lib managed directory any checksum for a jar if it exists
      // TODO(jvican): Support user-provided checksums
      val cachePath = cached.getAbsolutePath
      IvySbt.DefaultChecksums.foreach { checksum =>
        if (cachePath.endsWith(".jar")) {
          val cacheChecksum = new File(s"$cachePath.$checksum")
          if (cacheChecksum.exists()) {
            val toChecksum = new File(s"${to.getAbsolutePath}.$checksum")
            toCopy += ((cacheChecksum, toChecksum))
          }
        }
      }
    }

    to
  }

  private def retrieveTarget(
      conf: String,
      mid: ModuleID,
      art: Artifact,
      base: File,
      pattern: String
  ): File =
    new File(base, substitute(conf, mid, art, pattern))

  private def substitute(conf: String, mid: ModuleID, art: Artifact, pattern: String): String = {
    val mextra = IvySbt.javaMap(mid.extraAttributes, true)
    val aextra = IvySbt.extra(art, true)
    IvyPatternHelper.substitute(
      pattern,
      mid.organization,
      mid.name,
      mid.branchName.orNull,
      mid.revision,
      art.name,
      art.`type`,
      art.extension,
      conf,
      null,
      mextra,
      aextra
    )
  }

  import UpdateLogging.{ Quiet, Full, DownloadOnly, Default }
  import LogOptions.{ LOG_QUIET, LOG_DEFAULT, LOG_DOWNLOAD_ONLY }
  private def ivyLogLevel(level: UpdateLogging) =
    level match {
      case Quiet        => LOG_QUIET
      case DownloadOnly => LOG_DOWNLOAD_ONLY
      case Full         => LOG_DEFAULT
      case Default      => LOG_DOWNLOAD_ONLY
    }

  def publish(
      module: ModuleDescriptor,
      artifacts: Seq[(IArtifact, File)],
      resolver: DependencyResolver,
      overwrite: Boolean
  ): Unit = {
    if (artifacts.nonEmpty) {
      checkFilesPresent(artifacts)
      try {
        resolver.beginPublishTransaction(module.getModuleRevisionId(), overwrite);
        for ((artifact, file) <- artifacts)
          resolver.publish(artifact, file, overwrite)
        resolver.commitPublishTransaction()
      } catch {
        case e: Throwable =>
          try { resolver.abortPublishTransaction() } finally { throw e }
      }
    }
  }
  private[this] def checkFilesPresent(artifacts: Seq[(IArtifact, File)]): Unit = {
    val missing = artifacts filter { case (_, file) => !file.exists }
    if (missing.nonEmpty)
      sys.error(
        "Missing files for publishing:\n\t" + missing.map(_._2.getAbsolutePath).mkString("\n\t")
      )
  }
}
