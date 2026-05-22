package com.crispinlab.space.adapter.web.page

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.incoming.page.PageSearching
import com.crispinlab.space.application.port.incoming.page.PageSearching.Summary
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.SpaceAppControllerDescribeSpec
import com.crispinlab.user.testsupport.withAuth
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class PageSearchingControllerTest :
    SpaceAppControllerDescribeSpec(tag = "Page", body = {
        val useCase = mockk<PageSearching>()
        val controller = PageSearchingController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("페이지 검색") {
            it("정상 검색 시 200 과 페이지를 반환한다") {
                every { useCase.perform(any()) } returns
                    PageResult(
                        items =
                            listOf(
                                Summary(
                                    pageId = PageId(2L),
                                    spaceId = SpaceId(10L),
                                    title = "오늘의 회고",
                                    updatedAt = DUMMY_INSTANT
                                ),
                                Summary(
                                    pageId = PageId(1L),
                                    spaceId = SpaceId(10L),
                                    title = "어제의 회고",
                                    updatedAt = DUMMY_INSTANT
                                )
                            ),
                        page = 0,
                        size = 20,
                        totalElements = 2L
                    )

                controller
                    .`when`(
                        get("/v1/pages")
                            .withAuth()
                            .param("query", "회고")
                            .param("space", "10")
                            .param("tag", "100", "200")
                            .param("page", "0")
                            .param("size", "20")
                    ).then(
                        status().isOk,
                        jsonPath("$.items.length()").value(2),
                        jsonPath("$.items[0].pageId").value("2"),
                        jsonPath("$.items[0].spaceId").value("10"),
                        jsonPath("$.items[0].title").value("오늘의 회고"),
                        jsonPath("$.page").value(0),
                        jsonPath("$.size").value(20),
                        jsonPath("$.totalElements").value(2)
                    ).document(
                        authHeaderRequired(),
                        queryParameters(
                            "query" isParameterFor "검색 키워드 (제목·본문 LIKE)" isOptional true,
                            "space" isParameterFor "스페이스 ID 필터" isOptional true,
                            "tag" isParameterFor "태그 ID 필터 (다중 가능)" isOptional true
                        ).withPaging(),
                        responseFields {
                            "items".array("검색 결과 목록") {
                                "pageId".string("페이지 식별자")
                                "spaceId".string("소속 스페이스 식별자")
                                "title".string("제목")
                                "updatedAt".datetime("최근 갱신 시각")
                            }
                            "page".number("현재 페이지")
                            "size".number("페이지당 항목 수")
                            "totalElements".number("총 항목 수")
                            "totalPages".number("총 페이지 수")
                            "hasNext".boolean("다음 페이지 존재 여부")
                            "isEmpty".boolean("결과 비어 있음 여부")
                        }
                    )
            }

            it("필터/페이징 파라미터가 모두 비어도 기본값으로 200 을 반환한다") {
                every { useCase.perform(any()) } returns
                    PageResult(
                        items = emptyList(),
                        page = 0,
                        size = 20,
                        totalElements = 0L
                    )

                controller
                    .`when`(get("/v1/pages").withAuth())
                    .then(
                        status().isOk,
                        jsonPath("$.items.length()").value(0),
                        jsonPath("$.totalElements").value(0)
                    )
            }

            it("다중 tag 파라미터가 모두 UseCase Request 에 전달된다") {
                val requestSlot = slot<PageSearching.Request>()
                every { useCase.perform(capture(requestSlot)) } returns
                    PageResult(
                        items = emptyList(),
                        page = 0,
                        size = 20,
                        totalElements = 0L
                    )

                controller
                    .`when`(
                        get("/v1/pages")
                            .withAuth()
                            .param("tag", "100", "200", "300")
                    ).then(status().isOk)

                requestSlot.captured.tagIds.map { it.value } shouldBe listOf(100L, 200L, 300L)
            }

            it("space 형식이 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(
                        get("/v1/pages")
                            .withAuth()
                            .param("space", "abc")
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("tag 형식이 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(
                        get("/v1/pages")
                            .withAuth()
                            .param("tag", "xx")
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("page 가 음수면 400 을 반환한다") {
                controller
                    .`when`(
                        get("/v1/pages")
                            .withAuth()
                            .param("page", "-1")
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST"),
                        jsonPath("$.message").value("페이지 번호는 0 이상이어야 합니다.")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("size 가 허용 범위를 벗어나면 400 을 반환한다") {
                listOf("0", "201").forEach { invalidSize ->
                    controller
                        .`when`(
                            get("/v1/pages")
                                .withAuth()
                                .param("size", invalidSize)
                        ).then(
                            status().isBadRequest,
                            jsonPath("$.code").value("INVALID_REQUEST"),
                            jsonPath("$.message").value("페이지 크기는 1 이상 200 이하여야 합니다.")
                        )
                }
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("비로그인 상태에서도 200 으로 응답하고 Anonymous 컨텍스트로 UseCase 가 호출된다") {
                every { useCase.perform(any()) } returns
                    PageResult(
                        items = emptyList(),
                        page = 0,
                        size = 20,
                        totalElements = 0L
                    )

                controller
                    .`when`(get("/v1/pages"))
                    .then(
                        status().isOk,
                        jsonPath("$.items.length()").value(0)
                    )
                verify {
                    useCase.perform(
                        match { it.viewer == Viewer.Anonymous }
                    )
                }
            }
        }
    })
