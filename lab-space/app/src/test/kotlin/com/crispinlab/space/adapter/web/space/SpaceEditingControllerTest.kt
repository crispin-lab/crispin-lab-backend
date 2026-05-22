package com.crispinlab.space.adapter.web.space

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.requestFields
import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.space.application.port.incoming.space.SpaceEditing
import com.crispinlab.space.application.port.incoming.space.SpaceEditing.Result
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.SpaceAppControllerDescribeSpec
import com.crispinlab.user.testsupport.withAuth
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class SpaceEditingControllerTest :
    SpaceAppControllerDescribeSpec(tag = "Space", body = {
        val useCase = mockk<SpaceEditing>()
        val controller = SpaceEditingController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("스페이스 수정") {
            it("이름·설명을 변경하면 200 과 갱신 결과를 반환한다") {
                every { useCase.perform(any()) } returns
                    Result(
                        spaceId = SpaceId(1L),
                        name = "새 이름",
                        description = "새 설명",
                        visibility = SpaceVisibility.INTERNAL,
                        updatedAt = DUMMY_INSTANT
                    )

                controller
                    .`when`(
                        put("/v1/spaces/{spaceId}", 1)
                            .withAuth()
                            .body(mapOf("name" to "새 이름", "description" to "새 설명"))
                    ).then(
                        status().isOk,
                        jsonPath("$.spaceId").value("1"),
                        jsonPath("$.name").value("새 이름"),
                        jsonPath("$.description").value("새 설명"),
                        jsonPath("$.updatedAt").exists()
                    ).document(
                        authHeaderRequired(),
                        requestFields {
                            "name".string("변경할 이름", optional = true)
                            "description".string("변경할 설명", optional = true)
                            "visibility".string("변경할 공개 범위", optional = true)
                        },
                        responseFields {
                            "spaceId".string("스페이스 식별자")
                            "name".string("갱신된 이름")
                            "description".string("갱신된 설명")
                            "visibility".string("갱신된 공개 범위")
                            "updatedAt".datetime("갱신 시각")
                        }
                    )
            }

            it("Authorization 토큰이 없으면 401 을 반환한다") {
                controller
                    .`when`(
                        put("/v1/spaces/{spaceId}", 1)
                            .body(mapOf("name" to "새 이름", "description" to "새 설명"))
                    ).then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
