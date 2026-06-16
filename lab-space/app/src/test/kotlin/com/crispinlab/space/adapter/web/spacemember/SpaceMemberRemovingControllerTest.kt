package com.crispinlab.space.adapter.web.spacemember

import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberRemoving
import com.crispinlab.space.testsupport.SpaceAppControllerDescribeSpec
import com.crispinlab.user.testsupport.withAuth
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class SpaceMemberRemovingControllerTest :
    SpaceAppControllerDescribeSpec(tag = "SpaceMember", body = {
        val useCase = mockk<SpaceMemberRemoving>()
        val controller = SpaceMemberRemovingController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("스페이스 멤버 제거") {
            it("성공하면 204 를 반환한다") {
                every { useCase.perform(any()) } just runs

                controller
                    .`when`(
                        delete("/v1/spaces/{spaceId}/members/{userId}", 10, 200).withAuth()
                    ).then(status().isNoContent)
                    .document(authHeader(required = true))
            }

            it("Authorization 토큰이 없으면 401 을 반환한다") {
                controller
                    .`when`(delete("/v1/spaces/{spaceId}/members/{userId}", 10, 200))
                    .then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
