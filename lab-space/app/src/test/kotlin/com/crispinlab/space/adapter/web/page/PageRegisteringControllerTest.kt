package com.crispinlab.space.adapter.web.page

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.requestFields
import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.space.application.port.incoming.page.PageRegistering
import com.crispinlab.space.application.port.incoming.page.PageRegistering.Result
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.testsupport.SpaceAppControllerDescribeSpec
import com.crispinlab.user.testsupport.withAuth
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class PageRegisteringControllerTest :
    SpaceAppControllerDescribeSpec(tag = "Page", body = {
        val useCase = mockk<PageRegistering>()
        val controller = PageRegisteringController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("페이지 생성") {
            it("정상 생성 시 201 과 pageId 를 반환한다") {
                every {
                    useCase.perform(
                        match {
                            it.spaceId.value == 10L &&
                                it.parentPageId == null &&
                                it.title == "오늘의 회고" &&
                                it.content == "본문 [[wiki]]" &&
                                it.visibility == Visibility.DRAFT &&
                                it.viewer.userId.value == 100L
                        }
                    )
                } returns Result(pageId = PageId(42L))

                controller
                    .`when`(
                        post("/v1/pages")
                            .withAuth()
                            .body(
                                mapOf(
                                    "spaceId" to "10",
                                    "title" to "오늘의 회고",
                                    "content" to "본문 [[wiki]]",
                                    "visibility" to "DRAFT"
                                )
                            )
                    ).then(
                        status().isCreated,
                        jsonPath("$.pageId").value("42")
                    ).document(
                        authHeaderRequired(),
                        requestFields {
                            "spaceId".string("소속 스페이스 식별자")
                            "parentPageId".string("부모 페이지 식별자", optional = true)
                            "title".string("제목")
                            "content".string("본문 (위키링크 [[...]] 추출 대상)")
                            "visibility".string("공개 범위 (DRAFT / INTERNAL / PUBLIC)")
                        },
                        responseFields {
                            "pageId".string("생성된 페이지 식별자")
                        },
                        requestSchema = "PageRegisterRequest",
                        responseSchema = "PageRegisterResponse"
                    )
            }

            it("Authorization 토큰이 없으면 401 을 반환한다") {
                controller
                    .`when`(
                        post("/v1/pages")
                            .body(
                                mapOf(
                                    "spaceId" to "10",
                                    "title" to "테스트",
                                    "content" to "본문",
                                    "visibility" to "DRAFT"
                                )
                            )
                    ).then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
