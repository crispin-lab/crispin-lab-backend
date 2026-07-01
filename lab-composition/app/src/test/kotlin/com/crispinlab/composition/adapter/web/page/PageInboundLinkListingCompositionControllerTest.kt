package com.crispinlab.composition.adapter.web.page

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.composition.application.port.outgoing.user.UserHandleLookup
import com.crispinlab.composition.testsupport.CompositionAppControllerDescribeSpec
import com.crispinlab.space.application.port.incoming.page.PageInboundLinkListing
import com.crispinlab.space.application.port.incoming.page.PageInboundLinkListing.Summary
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageErrorCode
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
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

class PageInboundLinkListingCompositionControllerTest :
    CompositionAppControllerDescribeSpec(tag = "Page", body = {
        val useCase = mockk<PageInboundLinkListing>()
        val userHandleLookup = mockk<UserHandleLookup>()
        val controller = PageInboundLinkListingCompositionController(useCase, userHandleLookup)

        beforeEach {
            clearMocks(useCase, userHandleLookup)
            every { userHandleLookup.handlesOf(any()) } returns
                mapOf(UserId(100L) to "alice", UserId(200L) to "bob")
        }

        describe("페이지 인바운드 링크 목록 조회") {
            it("정상 응답 시 200 과 페이지를 반환한다") {
                every { useCase.perform(any()) } returns
                    PageResult(
                        items =
                            listOf(
                                Summary(
                                    pageId = PageId(11L),
                                    spaceId = SpaceId(10L),
                                    parentPageId = null,
                                    authorId = UserId(100L),
                                    title = "이전 회고",
                                    visibility = Visibility.PUBLIC,
                                    displayOrder = 0,
                                    updatedAt = DUMMY_INSTANT
                                ),
                                Summary(
                                    pageId = PageId(12L),
                                    spaceId = SpaceId(10L),
                                    parentPageId = PageId(11L),
                                    authorId = UserId(200L),
                                    title = "분기 회고",
                                    visibility = Visibility.INTERNAL,
                                    displayOrder = 1,
                                    updatedAt = DUMMY_INSTANT
                                )
                            ),
                        page = 0,
                        size = 20,
                        totalElements = 2L
                    )

                controller
                    .`when`(
                        get("/v1/pages/{pageId}/inbound", 42)
                            .withAuth()
                            .param("page", "0")
                            .param("size", "20")
                    ).then(
                        status().isOk,
                        jsonPath("$.items.length()").value(2),
                        jsonPath("$.items[0].pageId").value("11"),
                        jsonPath("$.items[0].authorHandle").value("alice"),
                        jsonPath("$.items[0].title").value("이전 회고"),
                        jsonPath("$.items[0].visibility").value("PUBLIC"),
                        jsonPath("$.items[1].parentPageId").value("11"),
                        jsonPath("$.totalElements").value(2),
                        jsonPath("$.hasNext").value(false)
                    ).document(
                        authHeader(required = false),
                        pagingParameters(),
                        responseFields {
                            "items".array("인바운드 링크 source 목록") {
                                "pageId".string("source 페이지 식별자")
                                "spaceId".string("source 페이지 소속 스페이스")
                                "parentPageId".string("부모 페이지 식별자", optional = true)
                                "authorId".string("작성자 식별자")
                                "authorHandle".string("작성자 핸들 — 알 수 없으면 빈 문자열")
                                "title".string("source 페이지 제목")
                                "visibility".string("PUBLIC / INTERNAL / DRAFT")
                                "displayOrder".number("스페이스 내 정렬 순서")
                                "updatedAt".datetime("source 페이지 최종 수정 시각")
                            }
                            "page".number("현재 페이지")
                            "size".number("페이지당 항목 수")
                            "totalElements".number("총 항목 수")
                            "totalPages".number("총 페이지 수")
                            "hasNext".boolean("다음 페이지 존재 여부")
                            "isEmpty".boolean("결과 비어 있음 여부")
                        },
                        responseSchema = "PageInboundLinkListResponse"
                    )
            }

            it("distinct authorIds 에 대해 handlesOf 를 정확히 1회 batch 호출한다") {
                every { useCase.perform(any()) } returns
                    PageResult(
                        items =
                            listOf(
                                Summary(
                                    pageId = PageId(11L),
                                    spaceId = SpaceId(10L),
                                    parentPageId = null,
                                    authorId = UserId(100L),
                                    title = "a",
                                    visibility = Visibility.PUBLIC,
                                    displayOrder = 0,
                                    updatedAt = DUMMY_INSTANT
                                ),
                                Summary(
                                    pageId = PageId(12L),
                                    spaceId = SpaceId(10L),
                                    parentPageId = null,
                                    authorId = UserId(100L),
                                    title = "b",
                                    visibility = Visibility.PUBLIC,
                                    displayOrder = 1,
                                    updatedAt = DUMMY_INSTANT
                                ),
                                Summary(
                                    pageId = PageId(13L),
                                    spaceId = SpaceId(10L),
                                    parentPageId = null,
                                    authorId = UserId(200L),
                                    title = "c",
                                    visibility = Visibility.PUBLIC,
                                    displayOrder = 2,
                                    updatedAt = DUMMY_INSTANT
                                )
                            ),
                        page = 0,
                        size = 20,
                        totalElements = 3L
                    )

                controller
                    .`when`(get("/v1/pages/{pageId}/inbound", 42).withAuth())
                    .then(status().isOk)
                verify(exactly = 1) {
                    userHandleLookup.handlesOf(setOf(UserId(100L), UserId(200L)))
                }
            }

            it("author 가 삭제된 사용자이면 authorHandle 은 빈 문자열로 응답한다") {
                every { userHandleLookup.handlesOf(any()) } returns emptyMap()
                every { useCase.perform(any()) } returns
                    PageResult(
                        items =
                            listOf(
                                Summary(
                                    pageId = PageId(11L),
                                    spaceId = SpaceId(10L),
                                    parentPageId = null,
                                    authorId = UserId(999L),
                                    title = "삭제된 사용자가 쓴 글",
                                    visibility = Visibility.PUBLIC,
                                    displayOrder = 0,
                                    updatedAt = DUMMY_INSTANT
                                )
                            ),
                        page = 0,
                        size = 20,
                        totalElements = 1L
                    )

                controller
                    .`when`(get("/v1/pages/{pageId}/inbound", 42).withAuth())
                    .then(
                        status().isOk,
                        jsonPath("$.items[0].authorHandle").value("")
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
                    .`when`(get("/v1/pages/{pageId}/inbound", 42).withAuth())
                    .then(
                        status().isOk,
                        jsonPath("$.totalElements").value(0)
                    )
            }

            it("target 페이지가 없거나 권한이 없으면 404 를 반환한다") {
                every { useCase.perform(any()) } throws
                    NotFoundException(PageErrorCode.PAGE_NOT_FOUND)

                controller
                    .`when`(get("/v1/pages/{pageId}/inbound", 999).withAuth())
                    .then(
                        status().isNotFound,
                        jsonPath("$.code").value("PAGE_NOT_FOUND")
                    )
            }

            it("pageId 형식이 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(
                        get("/v1/pages/{pageId}/inbound", "not-a-number").withAuth()
                    ).then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("비로그인 상태에서도 PUBLIC target 의 인바운드 목록은 200 으로 응답한다") {
                every { useCase.perform(any()) } returns
                    PageResult(
                        items = emptyList(),
                        page = 0,
                        size = 20,
                        totalElements = 0L
                    )

                controller
                    .`when`(get("/v1/pages/{pageId}/inbound", 42))
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
                        get("/v1/pages/{pageId}/inbound", 42)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-valid-token")
                    ).then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
