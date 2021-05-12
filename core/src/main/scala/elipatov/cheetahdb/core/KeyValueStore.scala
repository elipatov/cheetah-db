package elipatov.cheetahdb.core

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats._
import cats.data.OptionT
import cats.effect._
import cats.effect.concurrent.Ref
import cats.syntax.all._

import scala.collection.mutable.Map

trait CRDT[F[_], C[_], V] {
  def get: F[V]
  def modify(value: V): F[Unit]
  def getState(): F[C[V]]
  def merge(others: C[V]): F[Unit]
}

trait KeyValueStore[F[_], K, V] {
  def get(key: K): F[Option[V]]
  def put(key: K, value: V): F[Unit]
}

private abstract class InMemoryCRDTStore[F[_]: Monad, C[_], K, V](
    store: Ref[F, Map[K, CRDT[F, C, V]]],
    newCRDT: () => CRDT[F, C, V]
) extends KeyValueStore[F, K, V] {
  override def get(key: K): F[Option[V]] =
    store.modify(m => (m, m.get(key).traverse(_.get))).flatten

  override def put(key: K, value: V): F[Unit] =
    store.update(m => {
      if(!m.contains(key)){
        m(key) = newCRDT()
      }
      m(key).modify(value)
      m
    })
}

object InMemoryCRDTStore {
  def of[F[_]: Sync, C[_], K, V](newCRDT: () => CRDT[F, C, V]): F[KeyValueStore[F, K, V]] = {
    for {
      store <- Ref.of[F, Map[K, CRDT[F, C, V]]](Map.empty)
    } yield new InMemoryCRDTStore[F, C, K, V](store, newCRDT) {}
  }
}
