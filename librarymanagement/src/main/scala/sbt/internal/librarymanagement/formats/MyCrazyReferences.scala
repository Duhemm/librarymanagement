package sbt.internal.librarymanagement.formats

import scala.collection.mutable.Map

object MyCrazyReferences {

  private val references: Map[String, Any] = Map.empty

  def apply[T](key: String): T = synchronized { references(key).asInstanceOf[T] }
  def referenced[T](value: T): String = synchronized {
    val key = java.util.UUID.randomUUID.toString
    references(key) = value
    key
  }
}
