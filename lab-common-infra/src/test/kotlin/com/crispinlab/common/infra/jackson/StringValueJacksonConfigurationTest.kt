package com.crispinlab.common.infra.jackson

import com.crispinlab.common.domain.StringValue
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class StringValueJacksonConfigurationTest :
    DescribeSpec({
        val module: SimpleModule = StringValueJacksonConfiguration().stringValueJacksonModule()
        val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(module)

        describe("StringValue 직렬화") {
            it("StringValue 구현체는 underlying String 그대로 직렬화한다") {
                val json: String = objectMapper.writeValueAsString(SampleHandle(value = "alice"))

                json shouldBe "\"alice\""
            }

            it("Response 객체 안의 StringValue 필드도 String 으로 노출된다") {
                val response: SampleResponse =
                    SampleResponse(
                        handle = SampleHandle(value = "bob"),
                        email = SampleEmail(value = "bob@example.com")
                    )

                val json: String = objectMapper.writeValueAsString(response)

                json shouldBe """{"handle":"bob","email":"bob@example.com"}"""
            }

            it("nullable StringValue 필드가 null 이면 JSON null 로 나간다") {
                val response: NullableResponse =
                    NullableResponse(
                        handle = SampleHandle(value = "carol"),
                        nickname = null
                    )

                val json: String = objectMapper.writeValueAsString(response)

                json shouldBe """{"handle":"carol","nickname":null}"""
            }

            it("List<StringValue> 의 모든 원소가 String 으로 노출된다") {
                val response: CollectionResponse =
                    CollectionResponse(
                        handles = listOf(SampleHandle("a"), SampleHandle("b"), SampleHandle("c"))
                    )

                val json: String = objectMapper.writeValueAsString(response)

                json shouldBe """{"handles":["a","b","c"]}"""
            }

            it("super type StringValue 등록만으로 모든 sub type 이 같은 정책으로 직렬화된다") {
                val response: MixedSubtypeResponse =
                    MixedSubtypeResponse(
                        handle = SampleHandle(value = "x"),
                        email = SampleEmail(value = "y@z.com")
                    )

                val json: String = objectMapper.writeValueAsString(response)

                json shouldBe """{"handle":"x","email":"y@z.com"}"""
            }
        }
    }) {
    private data class SampleHandle(
        override val value: String
    ) : StringValue

    private data class SampleEmail(
        override val value: String
    ) : StringValue

    private data class SampleResponse(
        val handle: SampleHandle,
        val email: SampleEmail
    )

    private data class NullableResponse(
        val handle: SampleHandle,
        val nickname: SampleHandle?
    )

    private data class CollectionResponse(
        val handles: List<StringValue>
    )

    private data class MixedSubtypeResponse(
        val handle: SampleHandle,
        val email: SampleEmail
    )
}
