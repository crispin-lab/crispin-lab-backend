package com.crispinlab.space.adapter.web.page

import com.crispinlab.space.application.port.incoming.page.PageDeleting
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

class PageDeletingControllerTest :
    SpaceAppControllerDescribeSpec(tag = "Page", body = {
        val useCase = mockk<PageDeleting>()
        val controller = PageDeletingController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("페이지 삭제") {
            it("성공하면 204 를 반환한다") {
                every { useCase.perform(any()) } just runs

                controller
                    .`when`(
                        delete("/v1/pages/{pageId}", 1).withAuth()
                    ).then(status().isNoContent)
                    .document(authHeaderRequired())
            }

            it("Authorization 토큰이 없으면 401 을 반환한다") {
                controller
                    .`when`(delete("/v1/pages/{pageId}", 1))
                    .then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("pageId 형식이 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(delete("/v1/pages/{pageId}", "not-a-number").withAuth())
                    .then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
