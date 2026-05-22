package com.crispinlab.space.adapter.web.space

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.space.application.port.incoming.space.SpaceGetting
import com.crispinlab.space.application.port.incoming.space.SpaceGetting.Result
import com.crispinlab.space.domain.space.SpaceErrorCode
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.SpaceAppControllerDescribeSpec
import com.crispinlab.user.testsupport.withAuth
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
                        spaceId = SpaceId(1L),
                        name = "팀 위키",
                        description = "공유 공간",
                        visibility = SpaceVisibility.INTERNAL,
                        createdAt = DUMMY_INSTANT,
                        updatedAt = DUMMY_INSTANT
                    )

                controller
                    .`when`(
                        get("/v1/spaces/{spaceId}", 1).withAuth()
                    ).then(
                        status().isOk,
                        jsonPath("$.spaceId").value("1"),
                        jsonPath("$.name").value("팀 위키")
                    ).document(
                        authHeaderRequired(),
                        responseFields {
                            "spaceId".string("스페이스 식별자")
                            "name".string("이름")
                            "description".string("설명")
                            "visibility".string("공개 범위")
                            "createdAt".datetime("생성 시각")
                            "updatedAt".datetime("최근 갱신 시각")
                        }
                    )
            }

            it("없으면 404 를 반환한다") {
                every { useCase.perform(any()) } throws
                    NotFoundException(SpaceErrorCode.SPACE_NOT_FOUND)

                controller
                    .`when`(
                        get("/v1/spaces/{spaceId}", 999).withAuth()
                    ).then(
                        status().isNotFound,
                        jsonPath("$.code").value("SPACE_NOT_FOUND"),
                        jsonPath("$.message").value("스페이스를 찾을 수 없습니다.")
                    )
            }

            it("비로그인 상태에서도 PUBLIC 스페이스는 200 으로 응답한다") {
                every { useCase.perform(any()) } returns
                    Result(
                        spaceId = SpaceId(1L),
                        name = "공개 스페이스",
                        description = "누구나 볼 수 있음",
                        visibility = SpaceVisibility.PUBLIC,
                        createdAt = DUMMY_INSTANT,
                        updatedAt = DUMMY_INSTANT
                    )

                controller
                    .`when`(get("/v1/spaces/{spaceId}", 1))
                    .then(
                        status().isOk,
                        jsonPath("$.visibility").value("PUBLIC")
                    )
                verify {
                    useCase.perform(
                        match { it.currentUserId == null }
                    )
                }
            }
        }
    })
