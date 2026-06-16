package com.crispinlab.space.adapter.web.page

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.incoming.page.PageRevisionListing
import com.crispinlab.space.application.port.incoming.page.PageRevisionListing.Summary
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageErrorCode
import com.crispinlab.space.domain.page.PageId
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

class PageRevisionListingControllerTest :
    SpaceAppControllerDescribeSpec(tag = "Page", body = {
        val useCase = mockk<PageRevisionListing>()
        val controller = PageRevisionListingController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("페이지 리비전 목록 조회") {
            it("정상 응답 시 200 과 페이지를 반환한다") {
                every { useCase.perform(any()) } returns
                    PageResult(
                        items =
                            listOf(
                                Summary(
                                    revisionId = PageRevisionId(11L),
                                    pageId = PageId(10L),
                                    version = 2,
                                    title = "두 번째",
                                    authorId = UserId(100L),
                                    createdAt = DUMMY_INSTANT
                                ),
                                Summary(
                                    revisionId = PageRevisionId(12L),
                                    pageId = PageId(10L),
                                    version = 1,
                                    title = "초안",
                                    authorId = UserId(100L),
                                    createdAt = DUMMY_INSTANT
                                )
                            ),
                        page = 0,
                        size = 20,
                        totalElements = 2L
                    )

                controller
                    .`when`(
                        get("/v1/pages/{pageId}/revisions", 10)
                            .withAuth()
                            .param("page", "0")
                            .param("size", "20")
                    ).then(
                        status().isOk,
                        jsonPath("$.items.length()").value(2),
                        jsonPath("$.items[0].revisionId").value("11"),
                        jsonPath("$.items[0].version").value(2),
                        jsonPath("$.items[0].title").value("두 번째"),
                        jsonPath("$.totalElements").value(2),
                        jsonPath("$.hasNext").value(false)
                    ).document(
                        authHeader(required = false),
                        pagingParameters(),
                        responseFields {
                            "items".array("리비전 목록") {
                                "revisionId".string("리비전 식별자")
                                "pageId".string("소속 페이지 식별자")
                                "version".number("리비전 버전")
                                "title".string("그 시점의 제목")
                                "authorId".string("작성자 식별자")
                                "createdAt".datetime("기록된 시각")
                            }
                            "page".number("현재 페이지")
                            "size".number("페이지당 항목 수")
                            "totalElements".number("총 항목 수")
                            "totalPages".number("총 페이지 수")
                            "hasNext".boolean("다음 페이지 존재 여부")
                            "isEmpty".boolean("결과 비어 있음 여부")
                        },
                        responseSchema = "PageRevisionListResponse"
                    )
            }

            it("page/size 파라미터가 없어도 기본값으로 200 을 반환한다") {
                every { useCase.perform(any()) } returns
                    PageResult(
                        items = emptyList(),
                        page = 0,
                        size = 20,
                        totalElements = 0L
                    )

                controller
                    .`when`(get("/v1/pages/{pageId}/revisions", 10).withAuth())
                    .then(
                        status().isOk,
                        jsonPath("$.totalElements").value(0)
                    )
            }

            it("페이지가 없으면 404 를 반환한다") {
                every { useCase.perform(any()) } throws
                    NotFoundException(PageErrorCode.PAGE_NOT_FOUND)

                controller
                    .`when`(get("/v1/pages/{pageId}/revisions", 999).withAuth())
                    .then(
                        status().isNotFound,
                        jsonPath("$.code").value("PAGE_NOT_FOUND")
                    )
            }

            it("pageId 형식이 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(
                        get("/v1/pages/{pageId}/revisions", "not-a-number").withAuth()
                    ).then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("비로그인 상태에서도 PUBLIC 페이지의 리비전 목록은 200 으로 응답한다") {
                every { useCase.perform(any()) } returns
                    PageResult(
                        items = emptyList(),
                        page = 0,
                        size = 20,
                        totalElements = 0L
                    )

                controller
                    .`when`(get("/v1/pages/{pageId}/revisions", 10))
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
                        get("/v1/pages/{pageId}/revisions", 10)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-valid-token")
                    ).then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
