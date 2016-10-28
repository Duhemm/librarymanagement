package sbt.internal.librarymanagement.formats

import _root_.sjsonnew.JsonFormat
import org.apache.ivy.plugins.resolver.DependencyResolver

trait DependencyResolverFormat { self: sjsonnew.BasicJsonProtocol =>

  implicit lazy val DependencyResolverFormat: JsonFormat[DependencyResolver] =
    project(MyCrazyReferences.referenced _, (ref: String) => MyCrazyReferences.apply(ref, classOf[DependencyResolver]))

}
