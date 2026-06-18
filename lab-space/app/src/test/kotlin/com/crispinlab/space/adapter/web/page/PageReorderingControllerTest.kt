package com.crispinlab.space.adapter.web.page

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.requestFields
import com.crispinlab.space.application.port.incoming.page.PageReordering
import com.crispinlab.space.testsupport.SpaceAppControllerDescribeSpec
import com.crispinlab.user.testsupport.withAuth
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class PageReorderingControllerTest :
    SpaceAppControllerDescribeSpec(tag = "Page", body = {
        val useCase = mockk<PageReordering>()
        val controller = PageReorderingController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("페이지 순서 변경") {
            it("성공하면 204 를 반환한다") {
                every { useCase.perform(any()) } just runs

                controller
                    .`when`(
                        put("/v1/pages/{pageId}/order", 1)
                            .withAuth()
                            .body(mapOf("displayOrder" to 5))
                    ).then(status().isNoContent)
                    .document(
                        authHeader(required = true),
                        requestFields {
                            "displayOrder".number("동일한 부모 안에서의 새 표시 순서 (0 이상).")
                        },
                        requestSchema = "PageReorderRequest"
                    )
                verify(exactly = 1) {
                    useCase.perform(match { it.displayOrder == 5 })
                }
            }

            it("Authorization 토큰이 없으면 401 을 반환한다") {
                controller
                    .`when`(
                        put("/v1/pages/{pageId}/order", 1)
                            .body(mapOf("displayOrder" to 5))
                    ).then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("pageId 형식이 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(
                        put("/v1/pages/{pageId}/order", "not-a-number")
                            .withAuth()
                            .body(mapOf("displayOrder" to 5))
                    ).then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
