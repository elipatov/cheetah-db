package elipatov.cheetahdb.core

import cats.effect.IO
import org.scalacheck.Gen
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class GCounterStoreSpec extends AnyFreeSpec with ScalaCheckDrivenPropertyChecks with Matchers {
  val countGen = Gen.choose(1, 1000)
  val keyGen0  = Gen.alphaNumStr.map("0" + _)
  val keyGen1  = Gen.alphaNumStr.map("1" + _)

  "G-Counter Store" - {
    "creates new key" in {
      forAll(keyGen0, countGen) { (key: String, c: Int) =>
        val res = for {
          store <- InMemoryCRDTStore.gCounterStore[IO](0, 1)
          _     <- store.put(key, c)
          count <- store.get(key)
        } yield count

        res.unsafeRunSync() should be(Some(c))
      }
    }

    "updates existing key" in {
      forAll(keyGen0, countGen, countGen) { (key: String, c0: Int, c1: Int) =>
        val res = for {
          gCounter <- InMemoryCRDTStore.gCounterStore[IO](0, 1)
          _        <- gCounter.put(key, c0)
          _        <- gCounter.put(key, c1)
          count0   <- gCounter.get(key)
        } yield count0

        res.unsafeRunSync() should be(Some(c0 + c1))
      }
    }

    "creates multiple key" in {
      forAll(keyGen0, keyGen1, countGen, countGen) { (key0, key1, c0, c1) =>
        val io = for {
          gCounter <- InMemoryCRDTStore.gCounterStore[IO](0, 1)
          _        <- gCounter.put(key0, c0)
          _        <- gCounter.put(key1, c1)
          count0   <- gCounter.get(key0)
          count1   <- gCounter.get(key1)
        } yield (key0 -> count0, key1 -> count1)

        val actual = io.unsafeRunSync()
        actual shouldBe (key0 -> Some(c0), key1 -> Some(c1))
      }
    }

    "sync merges existing key" in {
      forAll(keyGen0, countGen, countGen) { (key: String, c0: Int, c1: Int) =>
        val io = for {
          node0  <- InMemoryCRDTStore.gCounterStore[IO](0, 2)
          _      <- node0.put(key, c0)
          _      <- node0.sync(Map(key -> Vector(0, c1)))
          count0 <- node0.get(key)
        } yield count0

        val actual = io.unsafeRunSync()
        actual should be(Some(c0 + c1))
      }
    }

    "sync creates missing key" in {
      forAll(keyGen0, countGen) { (key: String, c: Int) =>
        val io = for {
          node0  <- InMemoryCRDTStore.gCounterStore[IO](0, 2)
          _      <- node0.sync(Map(key -> Vector(0, c)))
          count0 <- node0.get(key)
        } yield count0

        val actual = io.unsafeRunSync()
        actual should be(Some(c))
      }
    }

  }

}
