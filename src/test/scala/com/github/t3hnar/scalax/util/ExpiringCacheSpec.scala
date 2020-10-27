package com.github.t3hnar.scalax.util

import org.specs2.mutable.Specification
import java.util.concurrent.TimeUnit
import org.specs2.specification.Scope
import scala.concurrent.duration.FiniteDuration

/**
 * @author Yaroslav Klymko
 */
class ExpiringCacheSpec extends Specification {
  "ExpiringCache" should {
    "clean expired values if get enough queries" in new ExpiringCacheScope {
      cache.map must haveSize(0)
      cache.queryCount mustEqual 0

      cache.put(0, "0")
      cache.get(0) must beSome("0")
      cache.map must haveSize(1)
      cache.queryCount mustEqual 1

      current = cache.unit.toMillis(cache.duration)

      cache.put(1, "1")
      cache.get(1) must beSome("1")
      cache.queryCount mustEqual 2

      (0 to cache.queryOverflow).foreach(_ => cache.get(3))

      cache.map.size must eventually(beEqualTo(1))
      cache.get(1) must beSome("1")
    }

    "not return expired values which are not cleaned" in new ExpiringCacheScope {
      cache.map must haveSize(0)
      cache.queryCount mustEqual 0

      cache.put(0, "0")
      cache.get(0) must beSome("0")
      cache.map.size must eventually(beEqualTo(1))

      current = cache.unit.toMillis(cache.duration)

      cache.get(0) must beNone
      cache.map.size must eventually(beEqualTo(1))
    }

    "remove entries" in new ExpiringCacheScope {
      cache.map must haveSize(0)
      cache.queryCount mustEqual 0

      cache.put(0, "0")
      cache.get(0) must beSome("0")

      cache.remove(0)
      cache.get(0) must beNone
    }

    "put should return previous stored value" in new ExpiringCacheScope {
      cache.map must haveSize(0)
      cache.queryCount mustEqual 0

      cache.put(0, "0") must beNone
      cache.get(0) must beSome("0")
      cache.put(0, "1") must beSome("0")
      cache.get(0) must beSome("1")
    }

    "this(duration: FiniteDuration, queryOverflow: Int)(implicit ec: ExecutionContext) should work" in new ExpiringCacheDurationConstructorScope {
      cache.duration must beEqualTo(finiteDuration.length)
      cache.unit must beEqualTo(finiteDuration.unit)
      cache.queryOverflow must beEqualTo(queryOverflow)
    }

    "currentMillis should contain current time in milliseconds" in new ExpiringCacheExposedMillisScope {
      import scala.language.reflectiveCalls

      scala.math.abs(
        cache.currentMillisExposed - System.currentTimeMillis) must beLessThan(10L)
    }

  }

  class ExpiringCacheScope extends Scope {
    var current = 0L
    val cache = new ExpiringCache[Int, String](1, TimeUnit.MILLISECONDS, 5) {
      override def currentMillis = current
    }
  }

  class ExpiringCacheDurationConstructorScope extends Scope {
    val finiteDuration = FiniteDuration(5, TimeUnit.MICROSECONDS)
    val queryOverflow = 1000
    var current = 0L
    val cache = new ExpiringCache[Int, String](finiteDuration, queryOverflow)(scala.concurrent.ExecutionContext.Implicits.global) {
      override def currentMillis = current
    }
  }

  class ExpiringCacheExposedMillisScope extends Scope {
    val cache = new ExpiringCache[Int, String](1, TimeUnit.MILLISECONDS, 5) {
      def currentMillisExposed = currentMillis
    }
  }
}

