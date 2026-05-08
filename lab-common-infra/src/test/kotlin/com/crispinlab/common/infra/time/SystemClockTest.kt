package com.crispinlab.common.infra.time

import com.crispinlab.common.time.Clock
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.ZoneOffset
import java.time.Clock as JavaClock

class SystemClockTest :
    DescribeSpec({
        describe("now") {
            it("주입된 java.time.Clock 의 시각을 그대로 반환한다") {
                val fixed: Instant = Instant.parse("2025-01-01T00:00:00Z")
                val clock: Clock = SystemClock(JavaClock.fixed(fixed, ZoneOffset.UTC))

                clock.now() shouldBe fixed
            }

            it("기본 생성자(systemUTC) 에서 호출 사이에 시각이 단조 증가한다") {
                val clock: Clock = SystemClock()

                val first: Instant = clock.now()
                val second: Instant = clock.now()

                (second >= first) shouldBe true
            }
        }
    })
