package sbt.librarymanagement

import java.net.URL

import sbt._
import sbt.internal.util.UnitSpec

object ResolverTest extends UnitSpec {

  "Resolver url" should "propagate pattern descriptorOptional and skipConsistencyCheck." in {
    val pats = Array("[orgPath]")
    val patsExpected = Array("http://foo.com/test/[orgPath]")
    val patterns = ResolverUtil.url("test", new URL("http://foo.com/test"))(new Patterns(pats, pats, /*isMavenCompatible =*/ false, /*descriptorOptional =*/ true, /*skipConsistencyCheck =*/ true)).patterns

    patterns.ivyPatterns shouldBe patsExpected
    patterns.artifactPatterns shouldBe patsExpected
    patterns.isMavenCompatible shouldBe false
    assert(patterns.skipConsistencyCheck)
    assert(patterns.descriptorOptional)
  }
}
