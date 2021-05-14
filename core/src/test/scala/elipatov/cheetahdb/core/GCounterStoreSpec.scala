package elipatov.cheetahdb.core

import cats.effect.IO
import cats.implicits.toTraverseOps
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class GCounterStoreSpec extends AnyFreeSpec with ScalaCheckDrivenPropertyChecks with Matchers {
  val replicasCount = 1

  "G-Counter Store" - {
    "creates new key" in {
      forAll { (key: String, c0: Byte) =>
        val res = for {
          gCounter <- InMemoryCRDTStore.gCounterStore[IO](0, replicasCount)
          _        <- gCounter.put(key, c0)
          count0   <- gCounter.get(key)
        } yield count0

        res.unsafeRunSync() == c0
      }
    }

    "updates existing key" in {
      forAll { (key: String, c0: Byte, c1: Byte) =>
        val res = for {
          gCounter <- InMemoryCRDTStore.gCounterStore[IO](0, replicasCount)
          _        <- gCounter.put(key, c0)
          _        <- gCounter.put(key, c1)
          count0   <- gCounter.get(key)
        } yield count0

        res.unsafeRunSync() == c0 + c1
      }
    }

    "creates multiple key" in {
      forAll { (key0: String, key1: String, c0: Byte, c1: Byte) =>
        val res = for {
          gCounter <- InMemoryCRDTStore.gCounterStore[IO](0, replicasCount)
          _        <- gCounter.put(key0, c0)
          _        <- gCounter.put(key1, c1)
          count0   <- gCounter.get(key0)
          count1   <- gCounter.get(key1)
        } yield (count0, count1)

        res.unsafeRunSync() == (c0, c1)
      }
    }
  }

}
