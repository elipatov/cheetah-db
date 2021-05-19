package elipatov.cheetahdb.core

import cats.Monad
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.all._

import scala.collection.immutable.HashSet
import scala.collection.mutable

trait Server[F[_]] {
  def getGCounter(key: String): F[Option[Long]]
  def putGCounter(key: String, value: Long): F[Unit]
  def syncGCounter(other: Map[String, Vector[Long]]): F[Unit]
}

class CRDTServer[F[_]: Monad](
                               gCounters: KeyValueStore[F, Vector, String, Long],
                               gCountersUpdates: Ref[F, HashSet[String]]
                             ) extends Server[F] {
  override def getGCounter(key: String): F[Option[Long]] = gCounters.get(key)

  override def putGCounter(key: String, value: Long): F[Unit] = {
    for {
      _ <- gCounters.put(key, value)
      _ <- gCountersUpdates.update(_.incl(key))
    } yield()
  }

  override def syncGCounter(other: Map[String, Vector[Long]]): F[Unit] = gCounters.sync(other)
}
