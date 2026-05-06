package com.crispinlab.common.id

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SnowflakeIdGeneratorTest :
    FunSpec({
        test("generates unique IDs across many calls") {
            val generator: IdGenerator = SnowflakeIdGenerator.create()
            val count = 10_000
            val ids: Set<Long> = (1..count).map { generator.next() }.toSet()
            ids.size shouldBe count
        }

        test("ids from a single instance are strictly increasing") {
            val generator: IdGenerator = SnowflakeIdGenerator.create()
            val ids: List<Long> = (1..1_000).map { generator.next() }
            ids.zipWithNext().all { (a, b) -> b > a } shouldBe true
        }

        test("ofNode returns positive ids") {
            val generator: IdGenerator = SnowflakeIdGenerator.ofNode(1L)
            val id: Long = generator.next()
            (id > 0L) shouldBe true
        }
    })
