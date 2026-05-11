package com.crispinlab.space.adapter.web.space

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.requestFields
import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.space.application.port.incoming.space.SpaceRegistering
import com.crispinlab.space.application.port.incoming.space.SpaceRegistering.Result
import com.crispinlab.space.testsupport.SpaceControllerDescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class SpaceRegisteringControllerTest :
    SpaceControllerDescribeSpec(tag = "Space", body = {
        val useCase = mockk<SpaceRegistering>()
        val controller = SpaceRegisteringController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("스페이스 생성") {
            it("정상 생성 시 201 과 spaceId 를 반환한다") {
                every {
                    useCase.perform(
                        match {
                            it.name == "팀 위키" &&
                                it.description == "공유 공간" &&
                                it.currentUserId.value == 100L
                        }
                    )
                } returns Result(spaceId = "42")

                controller
                    .`when`(
                        post("/v1/spaces")
                            .withUserHeader()
                            .body(mapOf("name" to "팀 위키", "description" to "공유 공간"))
                    ).then(
                        status().isCreated,
                        jsonPath("$.spaceId").value("42")
                    ).document(
                        userHeaderRequired(),
                        requestFields {
                            "name".string("스페이스 이름")
                            "description".string("스페이스 설명")
                        },
                        responseFields {
                            "spaceId".string("생성된 스페이스 식별자")
                        }
                    )
            }

            it("X-User-Id 헤더가 없으면 400 을 반환한다") {
                controller
                    .`when`(
                        post("/v1/spaces")
                            .body(mapOf("name" to "팀 위키", "description" to "공유 공간"))
                    ).then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("X-User-Id 가 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(
                        post("/v1/spaces")
                            .withUserHeader(userId = "not-a-number")
                            .body(mapOf("name" to "팀 위키", "description" to "공유 공간"))
                    ).then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
