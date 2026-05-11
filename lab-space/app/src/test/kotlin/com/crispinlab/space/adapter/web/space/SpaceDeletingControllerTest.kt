package com.crispinlab.space.adapter.web.space

import com.crispinlab.space.application.port.incoming.space.SpaceDeleting
import com.crispinlab.space.testsupport.SpaceControllerDescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class SpaceDeletingControllerTest :
    SpaceControllerDescribeSpec(tag = "Space", body = {
        val useCase = mockk<SpaceDeleting>()
        val controller = SpaceDeletingController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("스페이스 삭제") {
            it("성공하면 204 를 반환한다") {
                every { useCase.perform(any()) } just runs

                controller
                    .`when`(
                        delete("/v1/spaces/{spaceId}", 1).withUserHeader()
                    ).then(status().isNoContent)
                    .document(userHeaderRequired())
            }

            it("X-User-Id 헤더가 없으면 400 을 반환한다") {
                controller
                    .`when`(delete("/v1/spaces/{spaceId}", 1))
                    .then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("X-User-Id 가 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(
                        delete("/v1/spaces/{spaceId}", 1).withUserHeader(userId = "not-a-number")
                    ).then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
