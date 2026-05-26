package com.crispinlab.space.adapter.web.spacemember

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.requestFields
import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberRoleChanging
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberRoleChanging.Result
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class SpaceMemberRoleChangingControllerTest :
    SpaceAppControllerDescribeSpec(tag = "SpaceMember", body = {
        val useCase = mockk<SpaceMemberRoleChanging>()
        val controller = SpaceMemberRoleChangingController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("스페이스 멤버 역할 변경") {
            it("정상 변경 시 200 과 갱신된 멤버 정보를 반환한다") {
                every {
                    useCase.perform(
                        match {
                            it.spaceId == SpaceId(10L) &&
                                it.targetUserId == UserId(200L) &&
                                it.role == SpaceMemberRole.OWNER
                        }
                    )
                } returns
                    Result(
                        spaceMemberId = SpaceMemberId(2L),
                        spaceId = SpaceId(10L),
                        userId = UserId(200L),
                        role = SpaceMemberRole.OWNER
                    )

                controller
                    .`when`(
                        put("/v1/spaces/{spaceId}/members/{userId}", 10, 200)
                            .withAuth()
                            .body(mapOf("role" to "OWNER"))
                    ).then(
                        status().isOk,
                        jsonPath("$.role").value("OWNER")
                    ).document(
                        authHeaderRequired(),
                        requestFields {
                            "role".string("새 역할")
                        },
                        responseFields {
                            "spaceMemberId".string("멤버 식별자")
                            "spaceId".string("스페이스 식별자")
                            "userId".string("사용자 식별자")
                            "role".string("갱신된 역할")
                        }
                    )
            }

            it("Authorization 토큰이 없으면 401 을 반환한다") {
                controller
                    .`when`(
                        put("/v1/spaces/{spaceId}/members/{userId}", 10, 200)
                            .body(mapOf("role" to "OWNER"))
                    ).then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
