package com.crispinlab.space.adapter.web.page

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.requestFields
import com.crispinlab.space.application.port.incoming.page.PageMoving
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

class PageMovingControllerTest :
    SpaceAppControllerDescribeSpec(tag = "Page", body = {
        val useCase = mockk<PageMoving>()
        val controller = PageMovingController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("페이지 부모 이동") {
            it("성공하면 204 를 반환한다") {
                every { useCase.perform(any()) } just runs

                controller
                    .`when`(
                        put("/v1/pages/{pageId}/parent", 1)
                            .withAuth()
                            .body(mapOf("parentPageId" to "999"))
                    ).then(status().isNoContent)
                    .document(
                        authHeader(required = true),
                        requestFields {
                            "parentPageId".string(
                                "새 부모 페이지 식별자. null 이면 루트로 이동.",
                                optional = true
                            )
                        },
                        requestSchema = "PageMoveRequest"
                    )
            }

            it("parentPageId 를 생략하면 루트로 이동된다") {
                every { useCase.perform(any()) } just runs

                controller
                    .`when`(
                        put("/v1/pages/{pageId}/parent", 1)
                            .withAuth()
                            .body(emptyMap<String, Any>())
                    ).then(status().isNoContent)
            }

            it("Authorization 토큰이 없으면 401 을 반환한다") {
                controller
                    .`when`(
                        put("/v1/pages/{pageId}/parent", 1)
                            .body(mapOf("parentPageId" to "999"))
                    ).then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("pageId 형식이 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(
                        put("/v1/pages/{pageId}/parent", "not-a-number")
                            .withAuth()
                            .body(mapOf("parentPageId" to "999"))
                    ).then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
