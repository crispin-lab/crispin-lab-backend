package com.crispinlab.space.adapter.web.space

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.space.application.port.incoming.space.SpaceGetting
import com.crispinlab.space.application.port.incoming.space.SpaceGetting.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceErrorCode
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.SpaceAppControllerDescribeSpec
import com.crispinlab.user.testsupport.withAuth
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hamcrest.Matchers.nullValue
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
                        canWrite = true,
                        viewerRole = SpaceMemberRole.OWNER,
                        createdAt = DUMMY_INSTANT,
                        updatedAt = DUMMY_INSTANT
                    )

                controller
                    .`when`(
                        get("/v1/spaces/{spaceId}", 1).withAuth()
                    ).then(
                        status().isOk,
                        jsonPath("$.spaceId").value("1"),
                        jsonPath("$.name").value("팀 위키"),
                        jsonPath("$.canWrite").value(true),
                        jsonPath("$.viewerRole").value("OWNER")
                    ).document(
                        authHeader(required = false),
                        responseFields {
                            "spaceId".string("스페이스 식별자")
                            "name".string("이름")
                            "description".string("설명")
                            "visibility".string("공개 범위")
                            "canWrite".boolean(
                                "viewer 가 본 스페이스에 페이지를 작성할 수 있는지 여부 (ADMIN / OWNER / MEMBER → true, VIEWER · 비멤버 · 비로그인 → false)"
                            )
                            "viewerRole".string(
                                "viewer 의 본 스페이스 내 역할 (OWNER / MEMBER / VIEWER). 비-스페이스멤버 · 비로그인 → null",
                                optional = true
                            )
                            "createdAt".datetime("생성 시각")
                            "updatedAt".datetime("최근 갱신 시각")
                        },
                        responseSchema = "SpaceGetResponse"
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

            it("비로그인 상태에서도 PUBLIC 스페이스는 200 으로 응답한다 — canWrite = false, viewerRole = null") {
                every { useCase.perform(any()) } returns
                    Result(
                        spaceId = SpaceId(1L),
                        name = "공개 스페이스",
                        description = "누구나 볼 수 있음",
                        visibility = SpaceVisibility.PUBLIC,
                        canWrite = false,
                        viewerRole = null,
                        createdAt = DUMMY_INSTANT,
                        updatedAt = DUMMY_INSTANT
                    )

                controller
                    .`when`(get("/v1/spaces/{spaceId}", 1))
                    .then(
                        status().isOk,
                        jsonPath("$.visibility").value("PUBLIC"),
                        jsonPath("$.canWrite").value(false),
                        jsonPath("$.viewerRole").value(nullValue())
                    )
                verify {
                    useCase.perform(
                        match { it.viewer == Viewer.Anonymous }
                    )
                }
            }
        }
    })
