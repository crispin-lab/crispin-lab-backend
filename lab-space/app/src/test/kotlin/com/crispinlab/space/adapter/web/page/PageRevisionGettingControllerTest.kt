package com.crispinlab.space.adapter.web.page

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.space.application.port.incoming.page.PageRevisionGetting
import com.crispinlab.space.application.port.incoming.page.PageRevisionGetting.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageErrorCode
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageRevisionErrorCode
import com.crispinlab.space.domain.page.PageRevisionId
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.SpaceAppControllerDescribeSpec
import com.crispinlab.user.domain.user.UserId
import com.crispinlab.user.testsupport.withAuth
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class PageRevisionGettingControllerTest :
    SpaceAppControllerDescribeSpec(tag = "Page", body = {
        val useCase = mockk<PageRevisionGetting>()
        val controller = PageRevisionGettingController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("페이지 리비전 단건 조회") {
            it("존재하면 200 과 정보를 반환한다") {
                every { useCase.perform(any()) } returns
                    Result(
                        revisionId = PageRevisionId(11L),
                        pageId = PageId(10L),
                        version = 2,
                        title = "두 번째",
                        content = "본문 v2",
                        authorId = UserId(100L),
                        createdAt = DUMMY_INSTANT
                    )

                controller
                    .`when`(
                        get("/v1/pages/{pageId}/revisions/{version}", 10, 2).withAuth()
                    ).then(
                        status().isOk,
                        jsonPath("$.revisionId").value("11"),
                        jsonPath("$.pageId").value("10"),
                        jsonPath("$.version").value(2),
                        jsonPath("$.title").value("두 번째"),
                        jsonPath("$.content").value("본문 v2")
                    ).document(
                        responseFields {
                            "revisionId".string("리비전 식별자")
                            "pageId".string("소속 페이지 식별자")
                            "version".number("리비전 버전")
                            "title".string("그 시점의 제목")
                            "content".string("그 시점의 본문")
                            "authorId".string("작성자 식별자")
                            "createdAt".datetime("기록된 시각")
                        }
                    )
            }

            it("페이지 권한이 없으면 PAGE_NOT_FOUND") {
                every { useCase.perform(any()) } throws
                    NotFoundException(PageErrorCode.PAGE_NOT_FOUND)

                controller
                    .`when`(
                        get("/v1/pages/{pageId}/revisions/{version}", 999, 1).withAuth()
                    ).then(
                        status().isNotFound,
                        jsonPath("$.code").value("PAGE_NOT_FOUND")
                    )
            }

            it("리비전이 없으면 PAGE_REVISION_NOT_FOUND") {
                every { useCase.perform(any()) } throws
                    NotFoundException(PageRevisionErrorCode.PAGE_REVISION_NOT_FOUND)

                controller
                    .`when`(
                        get("/v1/pages/{pageId}/revisions/{version}", 10, 9999).withAuth()
                    ).then(
                        status().isNotFound,
                        jsonPath("$.code").value("PAGE_REVISION_NOT_FOUND")
                    )
            }

            it("version 이 0 이하이면 400 을 반환한다") {
                controller
                    .`when`(
                        get("/v1/pages/{pageId}/revisions/{version}", 10, 0).withAuth()
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.message").value("리비전 버전은 1 이상이어야 합니다.")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("pageId 형식이 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(
                        get("/v1/pages/{pageId}/revisions/{version}", "not-a-number", 1)
                            .withAuth()
                    ).then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("비로그인 상태에서도 PUBLIC 페이지의 리비전은 200 으로 응답한다") {
                every { useCase.perform(any()) } returns
                    Result(
                        revisionId = PageRevisionId(11L),
                        pageId = PageId(10L),
                        version = 1,
                        title = "초안",
                        content = "본문",
                        authorId = UserId(100L),
                        createdAt = DUMMY_INSTANT
                    )

                controller
                    .`when`(get("/v1/pages/{pageId}/revisions/{version}", 10, 1))
                    .then(status().isOk)
                verify {
                    useCase.perform(
                        match { it.viewer == Viewer.Anonymous }
                    )
                }
            }

            it("Authorization 헤더가 잘못되면 401 로 fail-fast 한다") {
                controller
                    .`when`(
                        get("/v1/pages/{pageId}/revisions/{version}", 10, 1)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-valid-token")
                    ).then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
