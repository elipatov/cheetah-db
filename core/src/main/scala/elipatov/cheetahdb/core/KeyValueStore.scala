package elipatov.cheetahdb.core

import cats._
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.all._
import cats.effect.syntax.all._

import scala.collection.mutable.Map

trait CRDT[+F[_], C[_], V] {
  def get: F[V]
  def modify(value: V): F[Unit]
  def getState(): F[C[V]]
  def merge(others: C[V]): F[Unit]
}

trait KeyValueStore[+F[_], K, V] {
  def get(key: K): F[Option[V]]
  def put(key: K, value: V): F[Unit]
}

abstract class InMemoryCRDTStore[+F[_]: Monad, C[_], K, V](
    store: Ref[F, Map[K, CRDT[F, C, V]]],
    newCRDT: () => F[CRDT[F, C, V]]
) extends KeyValueStore[F, K, V] {
  override def get(key: K): F[Option[V]] =
    store.modify(m => (m, m.get(key).traverse(_.get))).flatten

  override def put(key: K, value: V): F[Unit] = {
    for {
      n <- newCRDT()
      _ <- store.update(m => {
        if (!m.contains(key)) {
          m(key) = n
        }
        m(key).modify(value)
        m
      })
    } yield()

    store.access

    store.update(m => {
      if (!m.contains(key)) {
        newCRDT().flatMap(x => {m(key) = x; Applicative[F].pure(())})
      }
      m(key).modify(value)
      m
    })

  }
}

object InMemoryCRDTStore {
  def gCounterStore[F[+_]: Sync](
      replicaId: Int,
      replicasCount: Int
  ): F[KeyValueStore[F, String, Long]] = {
    for {
      store <- Ref.of[F, Map[String, CRDT[F, Array, Long]]](Map.empty)
      create = () => GCounterCvRDT.of(replicaId, replicasCount)
    } yield new InMemoryCRDTStore[F, Array, String, Long](store, create) {}

  }
}
