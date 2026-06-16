package com.crispinlab.space.adapter.web.tag

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.incoming.tag.TagPopularityListing
import com.crispinlab.space.application.port.incoming.tag.TagPopularityListing.Request
import com.crispinlab.space.application.port.incoming.tag.TagPopularityListing.Request.Companion.DEFAULT_POPULAR_SIZE
import com.crispinlab.space.application.port.incoming.tag.TagPopularityListing.Summary
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.testsupport.SpaceAppControllerDescribeSpec
import com.crispinlab.user.testsupport.withAuth
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class TagPopularityListingControllerTest :
    SpaceAppControllerDescribeSpec(tag = "Tag", body = {
        val useCase = mockk<TagPopularityListing>()
        val controller = TagPopularityListingController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("across-space 인기 태그 조회") {
            it("정상 응답 시 200 과 페이지를 반환한다") {
                every { useCase.perform(any()) } returns
                    PageResult(
                        items =
                            listOf(
                                Summary(name = "kotlin", usageCount = 5L),
                                Summary(name = "spring", usageCount = 3L)
                            ),
                        page = 0,
                        size = DEFAULT_POPULAR_SIZE,
                        totalElements = 2L
                    )

                controller
                    .`when`(
                        get("/v1/tags/popular")
                            .withAuth()
                            .param("page", "0")
                            .param("size", "$DEFAULT_POPULAR_SIZE")
                    ).then(
                        status().isOk,
                        jsonPath("$.items.length()").value(2),
                        jsonPath("$.items[0].name").value("kotlin"),
                        jsonPath("$.items[0].usageCount").value(5),
                        jsonPath("$.items[1].name").value("spring"),
                        jsonPath("$.items[1].usageCount").value(3),
                        jsonPath("$.totalElements").value(2),
                        jsonPath("$.hasNext").value(false)
                    ).document(
                        authHeader(required = false),
                        pagingParameters(),
                        responseFields {
                            "items".array("인기 태그 목록 (사용 빈도 내림차순)") {
                                "name".string("태그 이름 (cross-space 합산)")
                                "usageCount".number("페이지 사용 횟수")
                            }
                            "page".number("현재 페이지")
                            "size".number("페이지당 항목 수")
                            "totalElements".number("총 항목 수")
                            "totalPages".number("총 페이지 수")
                            "hasNext".boolean("다음 페이지 존재 여부")
                            "isEmpty".boolean("결과 비어 있음 여부")
                        },
                        responseSchema = "TagPopularityListResponse"
                    )
            }

            it("page/size 파라미터가 없으면 기본값(0, 30) 으로 UseCase 가 호출된다") {
                val captured = slot<Request>()
                every { useCase.perform(capture(captured)) } returns
                    PageResult(
                        items = emptyList(),
                        page = 0,
                        size = DEFAULT_POPULAR_SIZE,
                        totalElements = 0L
                    )

                controller
                    .`when`(get("/v1/tags/popular").withAuth())
                    .then(
                        status().isOk,
                        jsonPath("$.items.length()").value(0)
                    )

                captured.captured.pageRequest.page shouldBe 0
                captured.captured.pageRequest.size shouldBe DEFAULT_POPULAR_SIZE
            }

            it("비로그인 상태에서도 200 으로 응답하고 Anonymous 컨텍스트로 UseCase 가 호출된다") {
                every { useCase.perform(any()) } returns
                    PageResult(
                        items = emptyList(),
                        page = 0,
                        size = DEFAULT_POPULAR_SIZE,
                        totalElements = 0L
                    )

                controller
                    .`when`(get("/v1/tags/popular"))
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

            it("옵셔널 endpoint 라도 Authorization 헤더가 잘못되면 401 로 fail-fast 한다") {
                controller
                    .`when`(
                        get("/v1/tags/popular")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-valid-token")
                    ).then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("size 가 100 을 넘으면 400 과 한국어 메시지를 반환한다") {
                controller
                    .`when`(
                        get("/v1/tags/popular")
                            .withAuth()
                            .param("size", "101")
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST"),
                        jsonPath("$.message")
                            .value("인기 태그 페이지 크기는 100 이하여야 합니다.")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("page 가 음수면 400 을 반환한다") {
                controller
                    .`when`(
                        get("/v1/tags/popular")
                            .withAuth()
                            .param("page", "-1")
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST"),
                        jsonPath("$.message").value("페이지 번호는 0 이상이어야 합니다.")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
