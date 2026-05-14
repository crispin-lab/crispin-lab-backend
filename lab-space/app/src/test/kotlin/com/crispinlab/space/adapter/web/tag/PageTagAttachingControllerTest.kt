package com.crispinlab.space.adapter.web.tag

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.requestFields
import com.crispinlab.space.application.port.incoming.tag.PageTagAttaching
import com.crispinlab.space.testsupport.SpaceAppControllerDescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class PageTagAttachingControllerTest :
    SpaceAppControllerDescribeSpec(tag = "Tag", body = {
        val useCase = mockk<PageTagAttaching>()
        val controller = PageTagAttachingController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("페이지에 태그 매핑") {
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
                        post("/v1/pages/{pageId}/tags", 10)
                            .withUserHeader()
                            .body(mapOf("tagId" to "200"))
                    ).then(status().isNoContent)
                    .document(
                        userHeaderRequired(),
                        requestFields {
                            "tagId".string("매핑할 태그 식별자")
                        }
                    )

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
                    .`when`(
                        post("/v1/pages/{pageId}/tags", 10)
                            .body(mapOf("tagId" to "200"))
                    ).then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("pageId 형식이 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(
                        post("/v1/pages/{pageId}/tags", "not-a-number")
                            .withUserHeader()
                            .body(mapOf("tagId" to "200"))
                    ).then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("tagId 형식이 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(
                        post("/v1/pages/{pageId}/tags", 10)
                            .withUserHeader()
                            .body(mapOf("tagId" to "not-a-number"))
                    ).then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
