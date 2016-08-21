package sbt.librarymanagement

import java.io.File
import sbt.internal.util.UnitSpec

class CrossVersionTest extends UnitSpec {
  "Cross version" should "return sbt API for xyz as None" in {
    CrossVersionUtil.sbtApiVersion("xyz") shouldBe None
  }
  it should "return sbt API for 0.12 as None" in {
    CrossVersionUtil.sbtApiVersion("0.12") shouldBe None
  }
  it should "return sbt API for 0.12.0-SNAPSHOT as None" in {
    CrossVersionUtil.sbtApiVersion("0.12.0-SNAPSHOT") shouldBe None
  }
  it should "return sbt API for 0.12.0-RC1 as Some((0, 12))" in {
    CrossVersionUtil.sbtApiVersion("0.12.0-RC1") shouldBe Some((0, 12))
  }
  it should "return sbt API for 0.12.0 as Some((0, 12))" in {
    CrossVersionUtil.sbtApiVersion("0.12.0") shouldBe Some((0, 12))
  }
  it should "return sbt API for 0.12.1-SNAPSHOT as Some((0, 12))" in {
    CrossVersionUtil.sbtApiVersion("0.12.1-SNAPSHOT") shouldBe Some((0, 12))
  }
  it should "return sbt API for 0.12.1-RC1 as Some((0, 12))" in {
    CrossVersionUtil.sbtApiVersion("0.12.1-RC1") shouldBe Some((0, 12))
  }
  it should "return sbt API for 0.12.1 as Some((0, 12))" in {
    CrossVersionUtil.sbtApiVersion("0.12.1") shouldBe Some((0, 12))
  }
  it should "return sbt API compatibility for 0.12.0-M1 as false" in {
    CrossVersionUtil.isSbtApiCompatible("0.12.0-M1") shouldBe false
  }
  it should "return sbt API compatibility for 0.12.0-RC1 as true" in {
    CrossVersionUtil.isSbtApiCompatible("0.12.0-RC1") shouldBe true
  }
  it should "return sbt API compatibility for 0.12.1-RC1 as true" in {
    CrossVersionUtil.isSbtApiCompatible("0.12.1-RC1") shouldBe true
  }
  it should "return binary sbt version for 0.11.3 as 0.11.3" in {
    CrossVersionUtil.binarySbtVersion("0.11.3") shouldBe "0.11.3"
  }
  it should "return binary sbt version for 0.12.0-M1 as 0.12.0-M1" in {
    CrossVersionUtil.binarySbtVersion("0.12.0-M1") shouldBe "0.12.0-M1"
  }
  it should "return binary sbt version for 0.12.0-RC1 as 0.12" in {
    CrossVersionUtil.binarySbtVersion("0.12.0-RC1") shouldBe "0.12"
  }
  it should "return binary sbt version for 0.12.0 as 0.12" in {
    CrossVersionUtil.binarySbtVersion("0.12.0") shouldBe "0.12"
  }
  it should "return binary sbt version for 0.12.1-SNAPSHOT as 0.12" in {
    CrossVersionUtil.binarySbtVersion("0.12.1-SNAPSHOT") shouldBe "0.12"
  }
  it should "return binary sbt version for 0.12.1-RC1 as 0.12" in {
    CrossVersionUtil.binarySbtVersion("0.12.1-RC1") shouldBe "0.12"
  }
  it should "return binary sbt version for 0.12.1 as 0.12" in {
    CrossVersionUtil.binarySbtVersion("0.12.1") shouldBe "0.12"
  }
  it should "return Scala API for xyz as None" in {
    CrossVersionUtil.scalaApiVersion("xyz") shouldBe None
  }
  it should "return Scala API for 2.10 as None" in {
    CrossVersionUtil.scalaApiVersion("2.10") shouldBe None
  }
  it should "return Scala API for 2.10.0-SNAPSHOT as None" in {
    CrossVersionUtil.scalaApiVersion("2.10.0-SNAPSHOT") shouldBe None
  }
  it should "return Scala API for 2.10.0-RC1 as None" in {
    CrossVersionUtil.scalaApiVersion("2.10.0-RC1") shouldBe None
  }
  it should "return Scala API for 2.10.0 as Some((2, 10))" in {
    CrossVersionUtil.scalaApiVersion("2.10.0") shouldBe Some((2, 10))
  }
  it should "return Scala API for 2.10.0-1 as Some((2, 10))" in {
    CrossVersionUtil.scalaApiVersion("2.10.0-1") shouldBe Some((2, 10))
  }
  it should "return Scala API for 2.10.1-SNAPSHOT as Some((2, 10))" in {
    CrossVersionUtil.scalaApiVersion("2.10.1-SNAPSHOT") shouldBe Some((2, 10))
  }
  it should "return Scala API for 2.10.1-RC1 as Some((2, 10))" in {
    CrossVersionUtil.scalaApiVersion("2.10.1-RC1") shouldBe Some((2, 10))
  }
  it should "return Scala API for 2.10.1 as Some((2, 10))" in {
    CrossVersionUtil.scalaApiVersion("2.10.1") shouldBe Some((2, 10))
  }
  it should "return Scala API compatibility for 2.10.0-M1 as false" in {
    CrossVersionUtil.isScalaApiCompatible("2.10.0-M1") shouldBe false
  }
  it should "return Scala API compatibility for 2.10.0-RC1 as false" in {
    CrossVersionUtil.isScalaApiCompatible("2.10.0-RC1") shouldBe false
  }
  it should "return Scala API compatibility for 2.10.1-RC1 as false" in {
    CrossVersionUtil.isScalaApiCompatible("2.10.1-RC1") shouldBe true
  }
  it should "return binary Scala version for 2.9.2 as 2.9.2" in {
    CrossVersionUtil.binaryScalaVersion("2.9.2") shouldBe "2.9.2"
  }
  it should "return binary Scala version for 2.10.0-M1 as 2.10.0-M1" in {
    CrossVersionUtil.binaryScalaVersion("2.10.0-M1") shouldBe "2.10.0-M1"
  }
  it should "return binary Scala version for 2.10.0-RC1 as 2.10.0-RC1" in {
    CrossVersionUtil.binaryScalaVersion("2.10.0-RC1") shouldBe "2.10.0-RC1"
  }
  it should "return binary Scala version for 2.10.0 as 2.10" in {
    CrossVersionUtil.binaryScalaVersion("2.10.0") shouldBe "2.10"
  }
  it should "return binary Scala version for 2.10.1-M1 as 2.10" in {
    CrossVersionUtil.binaryScalaVersion("2.10.1-M1") shouldBe "2.10"
  }
  it should "return binary Scala version for 2.10.1-RC1 as 2.10" in {
    CrossVersionUtil.binaryScalaVersion("2.10.1-RC1") shouldBe "2.10"
  }
  it should "return binary Scala version for 2.10.1 as 2.10" in {
    CrossVersionUtil.binaryScalaVersion("2.10.1") shouldBe "2.10"
  }
}
