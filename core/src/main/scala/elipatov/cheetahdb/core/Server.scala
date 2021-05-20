package elipatov.cheetahdb.core

import cats.Monad
import cats.effect.{IO, Sync}
import cats.effect.concurrent.Ref
import cats.syntax.all._

import scala.collection.immutable.HashSet
import scala.collection.mutable

trait Server[F[_]] {
  def getGCounter(key: String): F[Option[Long]]
  def putGCounter(key: String, value: Long): F[Unit]
  def sync(other: SyncState): F[Unit]
  def close(): F[Unit]
}

case class SyncState(nodeId: Int, gCounter: Map[String, Vector[Long]])

class CRDTServer[F[_]: Monad](
    gCounters: KeyValueStore[F, Vector, String, Long],
    gCountersUpdates: Ref[F, HashSet[String]]
) extends Server[F] {
  override def getGCounter(key: String): F[Option[Long]] = gCounters.get(key)

  override def putGCounter(key: String, value: Long): F[Unit] = {
    for {
      _ <- gCounters.put(key, value)
      _ <- gCountersUpdates.update(_.incl(key))
    } yield ()
  }

  private def syncGCounter(other: Map[String, Vector[Long]]): F[Unit] = gCounters.sync(other)

  override def sync(state: SyncState): F[Unit] = {
    for {
      _ <- syncGCounter(state.gCounter)
    } yield ()

  }

  override def close(): F[Unit] = ().pure[F]
}

object CRDTServer {
  def of[F[+_]: Sync](nodeId: Int, replicasCount: Int): F[Server[F]] = {
    for {
      gCounter <- InMemoryCRDTStore.gCounterStore[F](nodeId, replicasCount)
      gCtrKeys <- Ref.of[F, HashSet[String]](HashSet.empty)
    } yield new CRDTServer(gCounter, gCtrKeys)
  }
}
