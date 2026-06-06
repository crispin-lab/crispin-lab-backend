package com.crispinlab.space.adapter.web.page

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.requestFields
import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.space.application.port.incoming.page.PageEditing
import com.crispinlab.space.application.port.incoming.page.PageEditing.Result
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.SpaceAppControllerDescribeSpec
import com.crispinlab.user.testsupport.withAuth
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class PageEditingControllerTest :
    SpaceAppControllerDescribeSpec(tag = "Page", body = {
        val useCase = mockk<PageEditing>()
        val controller = PageEditingController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("페이지 수정") {
            it("제목·본문 변경 시 200 과 갱신 결과를 반환한다") {
                every { useCase.perform(any()) } returns
                    Result(
                        pageId = PageId(1L),
                        title = "새 제목",
                        version = 2,
                        updatedAt = DUMMY_INSTANT
                    )

                controller
                    .`when`(
                        put("/v1/pages/{pageId}", 1)
                            .withAuth()
                            .body(
                                mapOf(
                                    "title" to "새 제목",
                                    "content" to "새 본문 [[wiki]]",
                                    "visibility" to Visibility.PUBLIC.name
                                )
                            )
                    ).then(
                        status().isOk,
                        jsonPath("$.pageId").value("1"),
                        jsonPath("$.title").value("새 제목"),
                        jsonPath("$.version").value(2)
                    ).document(
                        authHeaderRequired(),
                        requestFields {
                            "title".string("새 제목")
                            "content".string("새 본문")
                            "visibility".string(
                                "변경할 공개 범위 (DRAFT / INTERNAL / PUBLIC). 생략 시 기존 값 유지.",
                                optional = true
                            )
                        },
                        responseFields {
                            "pageId".string("페이지 식별자")
                            "title".string("갱신된 제목")
                            "version".number("갱신 후 currentVersion")
                            "updatedAt".datetime("갱신 시각")
                        },
                        requestSchema = "PageEditRequest",
                        responseSchema = "PageEditResponse"
                    )
            }

            it("지원하지 않는 공개 범위 값이면 400 을 반환한다") {
                controller
                    .`when`(
                        put("/v1/pages/{pageId}", 1)
                            .withAuth()
                            .body(
                                mapOf(
                                    "title" to "새 제목",
                                    "content" to "본문",
                                    "visibility" to "SECRET"
                                )
                            )
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST"),
                        jsonPath("$.message").value("지원하지 않는 공개 범위입니다.")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("Authorization 토큰이 없으면 401 을 반환한다") {
                controller
                    .`when`(
                        put("/v1/pages/{pageId}", 1)
                            .body(mapOf("title" to "새 제목", "content" to "본문"))
                    ).then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("pageId 형식이 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(
                        put("/v1/pages/{pageId}", "not-a-number")
                            .withAuth()
                            .body(mapOf("title" to "새 제목", "content" to "본문"))
                    ).then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
