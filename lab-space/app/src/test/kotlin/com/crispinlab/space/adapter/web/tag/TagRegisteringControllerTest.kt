package com.crispinlab.space.adapter.web.tag

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.requestFields
import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.space.application.port.incoming.tag.TagRegistering
import com.crispinlab.space.application.port.incoming.tag.TagRegistering.Result
import com.crispinlab.space.domain.tag.TagId
import com.crispinlab.space.testsupport.SpaceAppControllerDescribeSpec
import com.crispinlab.user.testsupport.withAuth
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class TagRegisteringControllerTest :
    SpaceAppControllerDescribeSpec(tag = "Tag", body = {
        val useCase = mockk<TagRegistering>()
        val controller = TagRegisteringController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("태그 등록") {
            it("정상 생성 시 201 과 tagId 를 반환한다") {
                every {
                    useCase.perform(
                        match {
                            it.spaceId.value == 10L &&
                                it.name == "kotlin" &&
                                it.viewer.userId.value == 100L
                        }
                    )
                } returns Result(tagId = TagId(42L))

                controller
                    .`when`(
                        post("/v1/tags")
                            .withAuth()
                            .body(mapOf("spaceId" to "10", "name" to "kotlin"))
                    ).then(
                        status().isCreated,
                        jsonPath("$.tagId").value("42")
                    ).document(
                        authHeaderRequired(),
                        requestFields {
                            "spaceId".string("스페이스 식별자")
                            "name".string("태그 이름")
                        },
                        responseFields {
                            "tagId".string("생성된 태그 식별자")
                        },
                        requestSchema = "TagRegisterRequest",
                        responseSchema = "TagRegisterResponse"
                    )

                verify(exactly = 1) {
                    useCase.perform(
                        match {
                            it.spaceId.value == 10L &&
                                it.name == "kotlin" &&
                                it.viewer.userId.value == 100L
                        }
                    )
                }
            }

            it("Authorization 토큰이 없으면 401 을 반환한다") {
                controller
                    .`when`(
                        post("/v1/tags")
                            .body(mapOf("spaceId" to "10", "name" to "kotlin"))
                    ).then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("spaceId 형식이 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(
                        post("/v1/tags")
                            .withAuth()
                            .body(mapOf("spaceId" to "not-a-number", "name" to "kotlin"))
                    ).then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
