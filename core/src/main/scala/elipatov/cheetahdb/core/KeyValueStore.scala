package elipatov.cheetahdb.core

import cats._
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.all._
import cats.effect.syntax.all._

trait CRDT[+F[_], C[_], V] {
  def get: F[V]
  def modify(value: V): F[Unit]
  def getState(): F[C[V]]
  def merge(others: C[V]): F[Unit]
}

trait KeyValueStore[+F[_], C[_], K, V] {
  def get(key: K): F[Option[V]]
  def put(key: K, value: V): F[Unit]
  def sync(other: Map[K, C[V]]): F[Unit]
}

private final class InMemoryCRDTStore[+F[_]: Monad, C[_], K, V](
    store: Ref[F, Map[K, CRDT[F, C, V]]],
    newCRDT: F[() => CRDT[F, C, V]]
) extends KeyValueStore[F, C, K, V] {
  override def get(key: K): F[Option[V]] =
    store.modify(m => (m, m.get(key).traverse(_.get))).flatten

  override def put(key: K, value: V): F[Unit] = {
    for {
      ctr <- newCRDT
      crdt <- store.modify(m => {
        if (!m.contains(key)) {
          val crdt = ctr()
          (m.updated(key, crdt), crdt)
        } else (m, m(key))
      })
      _ <- crdt.modify(value)
    } yield ()

  }

  override def sync(other: Map[K, C[V]]): F[Unit] = {
    other.toList
      .traverse {
        case (key, state) => {
          for {
            ctr <- newCRDT
            crdt <- store.modify(m => {
              if (m.contains(key)) {
                (m, m(key))
              } else {
                val crdt = ctr()
                (m.updated(key, crdt), crdt)
              }
            })
            _ <- crdt.merge(state)
          } yield ()
        }
      }
      .map(_ => ())
  }
}

object InMemoryCRDTStore {
  def gCounterStore[F[+_]: Sync](
      nodeId: Int,
      nodesCount: Int
  ): F[KeyValueStore[F, Vector, String, Long]] = {
    Ref
      .of[F, Map[String, CRDT[F, Vector, Long]]](Map.empty)
      .map(state =>
        new InMemoryCRDTStore[F, Vector, String, Long](
          state,
          GCounterCvRDT.ctr(nodeId, nodesCount)
        )
      )

  }
}
