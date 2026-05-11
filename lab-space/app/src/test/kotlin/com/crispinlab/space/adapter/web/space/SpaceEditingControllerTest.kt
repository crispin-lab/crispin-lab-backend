package com.crispinlab.space.adapter.web.space

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.requestFields
import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.space.application.port.incoming.space.SpaceEditing
import com.crispinlab.space.application.port.incoming.space.SpaceEditing.Result
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.SpaceControllerDescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class SpaceEditingControllerTest :
    SpaceControllerDescribeSpec(tag = "Space", body = {
        val useCase = mockk<SpaceEditing>()
        val controller = SpaceEditingController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("스페이스 수정") {
            it("이름·설명을 변경하면 200 과 갱신 결과를 반환한다") {
                every { useCase.perform(any()) } returns
                    Result(
                        spaceId = "1",
                        name = "새 이름",
                        description = "새 설명",
                        updatedAt = DUMMY_INSTANT
                    )

                controller
                    .`when`(
                        put("/v1/spaces/{spaceId}", 1)
                            .withUserHeader()
                            .body(mapOf("name" to "새 이름", "description" to "새 설명"))
                    ).then(
                        status().isOk,
                        jsonPath("$.spaceId").value("1"),
                        jsonPath("$.name").value("새 이름"),
                        jsonPath("$.description").value("새 설명"),
                        jsonPath("$.updatedAt").exists()
                    ).document(
                        userHeaderRequired(),
                        requestFields {
                            "name".string("변경할 이름", optional = true)
                            "description".string("변경할 설명", optional = true)
                        },
                        responseFields {
                            "spaceId".string("스페이스 식별자")
                            "name".string("갱신된 이름")
                            "description".string("갱신된 설명")
                            "updatedAt".datetime("갱신 시각")
                        }
                    )
            }

            it("X-User-Id 헤더가 없으면 400 을 반환한다") {
                controller
                    .`when`(
                        put("/v1/spaces/{spaceId}", 1)
                            .body(mapOf("name" to "새 이름", "description" to "새 설명"))
                    ).then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("X-User-Id 가 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(
                        put("/v1/spaces/{spaceId}", 1)
                            .withUserHeader(userId = "not-a-number")
                            .body(mapOf("name" to "새 이름", "description" to "새 설명"))
                    ).then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
