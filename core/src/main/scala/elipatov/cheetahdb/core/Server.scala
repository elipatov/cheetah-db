package elipatov.cheetahdb.core

import cats.Monad
import cats.effect.Sync
import cats.syntax.all._

trait Server[F[_]] {
  def getGCounter(key: String): F[Option[Long]]
  def putGCounter(key: String, value: Long): F[Unit]
  def syncGCounter(other: Map[String, Vector[Long]])
}

class CRDTServer[F[_]: Monad](gCounters: F[KeyValueStore[F, Vector, String, Long]]) extends Server[F] {
  override def getGCounter(key: String): F[Option[Long]] = gCounters.flatMap(_.get(key))

  override def putGCounter(key: String, value: Long): F[Unit] = gCounters.flatMap(_.put(key, value))

  override def syncGCounter(other: Map[String, Vector[Long]]): Unit = gCounters.flatMap(_.sync(other))
}
