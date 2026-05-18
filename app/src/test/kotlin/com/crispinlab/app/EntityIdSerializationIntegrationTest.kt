package com.crispinlab.app

import com.crispinlab.common.domain.EntityId
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(TestcontainersConfig::class)
class EntityIdSerializationIntegrationTest : DescribeSpec() {
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    init {
        extensions(SpringExtension())

        describe("EntityId Jackson 직렬화 — production wiring") {
            it("Spring Boot 가 wiring 한 ObjectMapper 는 EntityId 를 String 으로 직렬화한다") {
                val json: String =
                    objectMapper.writeValueAsString(
                        SampleResponse(
                            id = SampleId(value = 42L),
                            parentId = null
                        )
                    )

                json shouldBe """{"id":"42","parentId":null}"""
            }

            it("snowflake 크기 Long 도 정밀도 손실 없이 String 으로 나간다") {
                val snowflake = 9_007_199_254_740_993L

                val json: String =
                    objectMapper.writeValueAsString(
                        SampleResponse(
                            id = SampleId(snowflake),
                            parentId = null
                        )
                    )

                json shouldBe """{"id":"9007199254740993","parentId":null}"""
            }
        }
    }

    private data class SampleId(
        override val value: Long
    ) : EntityId

    private data class SampleResponse(
        val id: SampleId,
        val parentId: SampleId?
    )
}
