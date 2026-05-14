package com.crispinlab.space.adapter.web.tag

import com.crispinlab.space.application.port.incoming.tag.PageTagDetaching
import com.crispinlab.space.testsupport.SpaceAppControllerDescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class PageTagDetachingControllerTest :
    SpaceAppControllerDescribeSpec(tag = "Tag", body = {
        val useCase = mockk<PageTagDetaching>()
        val controller = PageTagDetachingController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("페이지에서 태그 매핑 제거") {
            it("성공하면 204 를 반환한다") {
                every {
                    useCase.perform(
                        match {
                            it.pageId.value == 10L &&
                                it.tagId.value == 200L &&
                                it.currentUserId.value == 100L
                        }
                    )
                } just runs

                controller
                    .`when`(
                        delete("/v1/pages/{pageId}/tags/{tagId}", 10, 200).withUserHeader()
                    ).then(status().isNoContent)
                    .document(userHeaderRequired())

                verify(exactly = 1) {
                    useCase.perform(
                        match {
                            it.pageId.value == 10L &&
                                it.tagId.value == 200L
                        }
                    )
                }
            }

            it("X-User-Id 헤더가 없으면 400 을 반환한다") {
                controller
                    .`when`(delete("/v1/pages/{pageId}/tags/{tagId}", 10, 200))
                    .then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("tagId 형식이 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(
                        delete("/v1/pages/{pageId}/tags/{tagId}", 10, "not-a-number")
                            .withUserHeader()
                    ).then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
