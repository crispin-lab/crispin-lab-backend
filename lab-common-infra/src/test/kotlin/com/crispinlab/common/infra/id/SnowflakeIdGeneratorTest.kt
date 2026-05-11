package com.crispinlab.common.infra.id

import com.crispinlab.common.id.IdGenerator
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class SnowflakeIdGeneratorTest :
    DescribeSpec({
        describe("SnowflakeIdGenerator") {
            it("같은 인스턴스에서 호출한 ID 들은 서로 겹치지 않는다") {
                val generator: IdGenerator = SnowflakeIdGenerator()
                val count = 10_000
                val ids: Set<Long> = (1..count).map { generator.next() }.toSet()
                ids.size shouldBe count
            }

            it("같은 인스턴스에서 호출한 ID 들은 엄격히 증가한다") {
                val generator: IdGenerator = SnowflakeIdGenerator()
                val ids: List<Long> = (1..1_000).map { generator.next() }
                ids.zipWithNext().all { (a, b) -> b > a } shouldBe true
            }

            it("nodeId 를 지정해도 양수 ID 를 반환한다") {
                val generator: IdGenerator = SnowflakeIdGenerator(nodeId = 1L)
                val id: Long = generator.next()
                (id > 0L) shouldBe true
            }
        }
    })
