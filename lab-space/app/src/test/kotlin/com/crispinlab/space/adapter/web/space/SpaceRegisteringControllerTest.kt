package com.crispinlab.space.adapter.web.space

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.requestFields
import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.common.exception.ForbiddenException
import com.crispinlab.space.application.port.incoming.space.SpaceRegistering
import com.crispinlab.space.application.port.incoming.space.SpaceRegistering.Result
import com.crispinlab.space.domain.space.SpaceErrorCode
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.testsupport.SpaceAppControllerDescribeSpec
import com.crispinlab.user.domain.user.SystemRole
import com.crispinlab.user.testsupport.withAuth
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class SpaceRegisteringControllerTest :
    SpaceAppControllerDescribeSpec(tag = "Space", body = {
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
                                it.visibility.name == "INTERNAL" &&
                                it.currentUserId.value == 100L
                        }
                    )
                } returns Result(spaceId = SpaceId(42L))

                controller
                    .`when`(
                        post("/v1/spaces")
                            .withAuth()
                            .body(
                                mapOf(
                                    "name" to "팀 위키",
                                    "description" to "공유 공간",
                                    "visibility" to "INTERNAL"
                                )
                            )
                    ).then(
                        status().isCreated,
                        jsonPath("$.spaceId").value("42")
                    ).document(
                        authHeaderRequired(),
                        requestFields {
                            "name".string("스페이스 이름")
                            "description".string("스페이스 설명")
                            "visibility".string("공개 범위 (PUBLIC|INTERNAL)")
                        },
                        responseFields {
                            "spaceId".string("생성된 스페이스 식별자")
                        }
                    )
            }

            it("Authorization 토큰이 없으면 401 을 반환한다") {
                controller
                    .`when`(
                        post("/v1/spaces")
                            .body(
                                mapOf(
                                    "name" to "팀 위키",
                                    "description" to "공유 공간",
                                    "visibility" to "INTERNAL"
                                )
                            )
                    ).then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("USER 가 호출하면 403 과 SPACE_ADMIN_ONLY 를 반환한다") {
                every { useCase.perform(any()) } throws
                    ForbiddenException(SpaceErrorCode.SPACE_ADMIN_ONLY)

                controller
                    .`when`(
                        post("/v1/spaces")
                            .withAuth(role = SystemRole.USER)
                            .body(
                                mapOf(
                                    "name" to "팀 위키",
                                    "description" to "공유 공간",
                                    "visibility" to "INTERNAL"
                                )
                            )
                    ).then(
                        status().isForbidden,
                        jsonPath("$.code").value("SPACE_ADMIN_ONLY"),
                        jsonPath("$.message").value("관리자만 수행할 수 있습니다.")
                    )
            }
        }
    })
