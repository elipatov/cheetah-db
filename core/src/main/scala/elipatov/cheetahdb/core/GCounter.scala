package elipatov.cheetahdb.core

import cats.Monad
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits.catsSyntaxApplicativeId
import cats.syntax.all._

// G-counter in CRDT is a grow-only counter that only supports increment.
// It is implemented as a state-based CvRDT.
trait GCounter[+F[_]] extends CRDT[F, Vector, Long] {}

private final class GCounterCvRDT[+F[_]: Monad](
    replicaId: Int,
    counts: Vector[Ref[F, Long]]
) extends GCounter[F] {
  def modify(value: Long): F[Unit] = counts(replicaId).update(_ + value)

  override def get: F[Long] = counts.traverse(c => c.get).map(_.sum)

  override def getState(): F[Vector[Long]] = counts.traverse(_.get)

  override def merge(others: Vector[Long]): F[Unit] = {
    val ziped = counts.zip(others)
      ziped.traverse { case(ref, o) =>
        ref.update(math.max(_, o))
      }.map(_ => ())
  }
}

object GCounterCvRDT {
  def of[F[+_]: Sync](replicaId: Int, replicasCount: Int): F[GCounter[F]] = {
    new GCounterCvRDT[F](replicaId, Vector.fill(replicasCount)(Ref.unsafe[F, Long](0))).pure[F]
  }

  def ctr[F[+_]: Sync](replicaId: Int, replicasCount: Int): F[() => GCounter[F]] = {
    (() => new GCounterCvRDT[F](replicaId, Vector.fill(replicasCount)(Ref.unsafe[F, Long](0)))).pure[F]
  }

//  def ctr_[F[+_]: Sync](replicaId: Int): F[Vector[Long] => GCounter[F]] = {
//    val ctr = (s: Vector[Long]) =>
//      new GCounterCvRDT[F](replicaId, s.map(Ref.unsafe[F, Long](_)))
//    ctr.pure[F]
//  }
}
