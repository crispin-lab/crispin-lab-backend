package com.crispinlab.space.adapter.web.space

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.space.application.port.incoming.space.SpaceGetting
import com.crispinlab.space.application.port.incoming.space.SpaceGetting.Result
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.SpaceAppControllerDescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class SpaceGettingControllerTest :
    SpaceAppControllerDescribeSpec(tag = "Space", body = {
        val useCase = mockk<SpaceGetting>()
        val controller = SpaceGettingController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("스페이스 단건 조회") {
            it("존재하면 200 과 정보를 반환한다") {
                every { useCase.perform(any()) } returns
                    Result(
                        spaceId = "1",
                        name = "팀 위키",
                        description = "공유 공간",
                        createdAt = DUMMY_INSTANT,
                        updatedAt = DUMMY_INSTANT
                    )

                controller
                    .`when`(
                        get("/v1/spaces/{spaceId}", 1).withUserHeader()
                    ).then(
                        status().isOk,
                        jsonPath("$.spaceId").value("1"),
                        jsonPath("$.name").value("팀 위키")
                    ).document(
                        userHeaderRequired(),
                        responseFields {
                            "spaceId".string("스페이스 식별자")
                            "name".string("이름")
                            "description".string("설명")
                            "createdAt".datetime("생성 시각")
                            "updatedAt".datetime("최근 갱신 시각")
                        }
                    )
            }

            it("없으면 404 를 반환한다") {
                every { useCase.perform(any()) } throws NotFoundException("스페이스를 찾을 수 없습니다.")

                controller
                    .`when`(
                        get("/v1/spaces/{spaceId}", 999).withUserHeader()
                    ).then(
                        status().isNotFound,
                        jsonPath("$.message").value("스페이스를 찾을 수 없습니다.")
                    )
            }

            it("X-User-Id 헤더가 없으면 400 을 반환한다") {
                controller
                    .`when`(get("/v1/spaces/{spaceId}", 1))
                    .then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("X-User-Id 가 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(get("/v1/spaces/{spaceId}", 1).withUserHeader(userId = "not-a-number"))
                    .then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
