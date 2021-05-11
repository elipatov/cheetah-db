package elipatov.cheetahdb.core

import cats.effect.{Async, Concurrent, Timer}
import cats.effect.concurrent.Ref
import cats.implicits.catsSyntaxApplicativeId
import cats.Monad
import cats.syntax.all._
import cats.effect.syntax.all._
import cats.effect.concurrent.Ref
import cats.effect.{Async, Clock, Concurrent, ExitCode, IO, IOApp, Sync, Timer}
import cats.effect.implicits._

// G-counter in CRDT is a grow-only counter that only supports increment.
// It is implemented as a state-based CvRDT.
trait GCounter[F[_]] {
  def increment(): F[Unit]
  def count: F[Long]
  def getState(): F[Array[Long]]
  def merge(others: Array[Long]): F[Unit]
}

final class GCounterCvRDT[F[_]: Monad](replicaId: Int, counts: Ref[F, Array[Long]]) extends GCounter[F] {
  def increment(): F[Unit] =
    counts.update(cs => {
      cs(replicaId) += 1
      cs
    })

  override def count: F[Long] = counts.modify(cs => (cs, cs.sum))

  override def getState(): F[Array[Long]] = counts.get.map(cs => cs)

  override def merge(others: Array[Long]): F[Unit] =
    counts.update(cs => {
      others.zipWithIndex.foreach {
        case (v, i) =>
          if (v > cs(i)) {
            cs(i) = v
          }
      }
      cs
    })
}
object GCounterCvRDT {
  def of[F[_]: Sync](
      replicaId: Int,
      replicasCount: Int
  ): F[GCounter[F]] = {
    for {
      counts  <- Ref.of[F, Array[Long]](Array.ofDim(replicasCount))
      counter <- new GCounterCvRDT[F](replicaId, counts).pure[F]
    } yield counter
  }
}
