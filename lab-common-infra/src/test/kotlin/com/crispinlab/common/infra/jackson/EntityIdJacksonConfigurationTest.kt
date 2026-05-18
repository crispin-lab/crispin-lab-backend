package com.crispinlab.common.infra.jackson

import com.crispinlab.common.domain.EntityId
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class EntityIdJacksonConfigurationTest :
    DescribeSpec({
        val module: SimpleModule = EntityIdJacksonConfiguration().entityIdJacksonModule()
        val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(module)

        describe("EntityId 직렬화") {
            it("EntityId 구현체는 underlying Long 을 String 으로 직렬화한다") {
                val json: String = objectMapper.writeValueAsString(SampleId(value = 42L))

                json shouldBe "\"42\""
            }

            it("snowflake 크기의 Long 도 정밀도 손실 없이 String 으로 나간다") {
                val snowflake: Long = 9_007_199_254_740_993L
                val json: String = objectMapper.writeValueAsString(SampleId(value = snowflake))

                json shouldBe "\"9007199254740993\""
            }

            it("Response 객체 안의 EntityId 필드도 String 으로 노출된다") {
                val response: SampleResponse =
                    SampleResponse(
                        id = SampleId(value = 1L),
                        ownerId = OtherId(value = 2L)
                    )

                val json: String = objectMapper.writeValueAsString(response)

                json shouldBe """{"id":"1","ownerId":"2"}"""
            }

            it("nullable EntityId 필드가 null 이면 JSON null 로 나간다") {
                val response: NullableResponse =
                    NullableResponse(
                        id = SampleId(value = 1L),
                        parentId = null
                    )

                val json: String = objectMapper.writeValueAsString(response)

                json shouldBe """{"id":"1","parentId":null}"""
            }

            it("List<EntityId> 의 모든 원소가 String 으로 노출된다") {
                val response: CollectionResponse =
                    CollectionResponse(
                        ids = listOf(SampleId(1L), SampleId(2L), SampleId(3L))
                    )

                val json: String = objectMapper.writeValueAsString(response)

                json shouldBe """{"ids":["1","2","3"]}"""
            }

            it("super type EntityId 등록만으로 모든 sub type 이 같은 정책으로 직렬화된다") {
                val response: MixedSubtypeResponse =
                    MixedSubtypeResponse(
                        sampleId = SampleId(value = 10L),
                        otherId = OtherId(value = 20L)
                    )

                val json: String = objectMapper.writeValueAsString(response)

                json shouldBe """{"sampleId":"10","otherId":"20"}"""
            }
        }
    }) {
    private data class SampleId(
        override val value: Long
    ) : EntityId

    private data class OtherId(
        override val value: Long
    ) : EntityId

    private data class SampleResponse(
        val id: SampleId,
        val ownerId: OtherId
    )

    private data class NullableResponse(
        val id: SampleId,
        val parentId: SampleId?
    )

    private data class CollectionResponse(
        val ids: List<EntityId>
    )

    private data class MixedSubtypeResponse(
        val sampleId: SampleId,
        val otherId: OtherId
    )
}
