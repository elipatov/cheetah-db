package elipatov.cheetahdb.core

import cats.effect.IO
import cats.implicits.toTraverseOps
import org.scalacheck.Gen
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class GCounterStoreSpec extends AnyFreeSpec with ScalaCheckDrivenPropertyChecks with Matchers {
  val replicasCount = 1
  val countGen = Gen.choose(1, 1000)
  val keyGen = Gen.alphaNumStr

  "G-Counter Store" - {
    "creates new key" in {
      forAll(keyGen, countGen) { (key: String, c: Int) =>
        val res = for {
          store <- InMemoryCRDTStore.gCounterStore[IO](0, replicasCount)
          _     <- store.put(key, c)
          count <- store.get(key)
        } yield count

        res.unsafeRunSync() should be(Some(c))
      }
    }

    "updates existing key" in {
      forAll(keyGen, countGen, countGen) { (key: String, c0: Int, c1: Int) =>
        val res = for {
          gCounter <- InMemoryCRDTStore.gCounterStore[IO](0, replicasCount)
          _        <- gCounter.put(key, c0)
          _        <- gCounter.put(key, c1)
          count0   <- gCounter.get(key)
        } yield count0

        res.unsafeRunSync() should be(Some(c0 + c1))
      }
    }

    "creates multiple key" in {
      forAll(keyGen, keyGen, countGen, countGen) { (key0: String, key1: String, c0: Int, c1: Int) =>
        val res = for {
          gCounter <- InMemoryCRDTStore.gCounterStore[IO](0, replicasCount)
          _        <- gCounter.put(key0, c0)
          _        <- gCounter.put(key1, c1)
          count0   <- gCounter.get(key0)
          count1   <- gCounter.get(key1)
        } yield (count0, count1)

        res.unsafeRunSync() should be(Some(c0), Some(c1))
      }
    }
  }

}
