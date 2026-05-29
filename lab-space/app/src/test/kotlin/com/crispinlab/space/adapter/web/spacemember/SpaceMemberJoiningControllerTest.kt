package com.crispinlab.space.adapter.web.spacemember

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.requestFields
import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberJoining
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberJoining.Result
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.spacemember.SpaceMemberId
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.space.testsupport.SpaceAppControllerDescribeSpec
import com.crispinlab.user.domain.user.UserId
import com.crispinlab.user.testsupport.withAuth
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class SpaceMemberJoiningControllerTest :
    SpaceAppControllerDescribeSpec(tag = "SpaceMember", body = {
        val useCase = mockk<SpaceMemberJoining>()
        val controller = SpaceMemberJoiningController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("스페이스 멤버 가입") {
            it("자가 가입 시 201 과 멤버 정보를 반환한다") {
                every {
                    useCase.perform(
                        match {
                            it.spaceId == SpaceId(10L) &&
                                it.targetUserId == null &&
                                it.viewer.userId.value == 100L
                        }
                    )
                } returns
                    Result(
                        spaceMemberId = SpaceMemberId(42L),
                        spaceId = SpaceId(10L),
                        userId = UserId(100L),
                        role = SpaceMemberRole.MEMBER
                    )

                controller
                    .`when`(
                        post("/v1/spaces/{spaceId}/members", 10).withAuth()
                    ).then(
                        status().isCreated,
                        jsonPath("$.spaceMemberId").value("42"),
                        jsonPath("$.role").value("MEMBER")
                    ).document(
                        authHeaderRequired(),
                        responseFields {
                            "spaceMemberId".string("멤버 식별자")
                            "spaceId".string("스페이스 식별자")
                            "userId".string("사용자 식별자")
                            "role".string("부여된 역할")
                        },
                        requestSchema = "SpaceMemberJoinRequest",
                        responseSchema = "SpaceMemberJoinResponse"
                    )
            }

            it("body 에 userId 와 role 을 명시하면 OWNER 가 초대한다") {
                every { useCase.perform(any()) } returns
                    Result(
                        spaceMemberId = SpaceMemberId(43L),
                        spaceId = SpaceId(10L),
                        userId = UserId(200L),
                        role = SpaceMemberRole.VIEWER
                    )

                controller
                    .`when`(
                        post("/v1/spaces/{spaceId}/members", 10)
                            .withAuth()
                            .body(mapOf("userId" to "200", "role" to "VIEWER"))
                    ).then(
                        status().isCreated,
                        jsonPath("$.userId").value("200"),
                        jsonPath("$.role").value("VIEWER")
                    ).document(
                        requestFields {
                            "userId".string("초대 대상 사용자 식별자", optional = true)
                            "role".string("부여할 역할", optional = true)
                        },
                        requestSchema = "SpaceMemberJoinRequest",
                        responseSchema = "SpaceMemberJoinResponse"
                    )
            }

            it("Authorization 토큰이 없으면 401 을 반환한다") {
                controller
                    .`when`(post("/v1/spaces/{spaceId}/members", 10))
                    .then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
