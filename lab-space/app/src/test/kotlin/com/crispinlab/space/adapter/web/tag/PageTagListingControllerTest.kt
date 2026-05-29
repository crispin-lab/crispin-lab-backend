package com.crispinlab.space.adapter.web.tag

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.incoming.tag.PageTagListing
import com.crispinlab.space.application.port.incoming.tag.PageTagListing.Request
import com.crispinlab.space.application.port.incoming.tag.PageTagListing.Summary
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.tag.TagId
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.SpaceAppControllerDescribeSpec
import com.crispinlab.user.testsupport.withAuth
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class PageTagListingControllerTest :
    SpaceAppControllerDescribeSpec(tag = "Tag", body = {
        val useCase = mockk<PageTagListing>()
        val controller = PageTagListingController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("페이지 태그 목록 조회") {
            it("정상 응답 시 200 과 페이지를 반환한다") {
                every { useCase.perform(any()) } returns
                    PageResult(
                        items =
                            listOf(
                                Summary(
                                    tagId = TagId(1L),
                                    spaceId = SpaceId(10L),
                                    name = "kotlin",
                                    createdAt = DUMMY_INSTANT
                                ),
                                Summary(
                                    tagId = TagId(2L),
                                    spaceId = SpaceId(10L),
                                    name = "spring",
                                    createdAt = DUMMY_INSTANT
                                )
                            ),
                        page = 0,
                        size = 20,
                        totalElements = 2L
                    )

                controller
                    .`when`(
                        get("/v1/pages/{pageId}/tags", 10)
                            .withAuth()
                            .param("page", "0")
                            .param("size", "20")
                    ).then(
                        status().isOk,
                        jsonPath("$.items.length()").value(2),
                        jsonPath("$.items[0].tagId").value("1"),
                        jsonPath("$.items[0].spaceId").value("10"),
                        jsonPath("$.items[0].name").value("kotlin"),
                        jsonPath("$.totalElements").value(2),
                        jsonPath("$.hasNext").value(false)
                    ).document(
                        authHeaderRequired(),
                        pagingParameters(),
                        responseFields {
                            "items".array("태그 목록") {
                                "tagId".string("태그 식별자")
                                "spaceId".string("소속 스페이스 식별자")
                                "name".string("태그 이름")
                                "createdAt".datetime("생성 시각")
                            }
                            "page".number("현재 페이지")
                            "size".number("페이지당 항목 수")
                            "totalElements".number("총 항목 수")
                            "totalPages".number("총 페이지 수")
                            "hasNext".boolean("다음 페이지 존재 여부")
                            "isEmpty".boolean("결과 비어 있음 여부")
                        },
                        responseSchema = "PageTagListResponse"
                    )
            }

            it("page/size 파라미터가 없어도 기본값을 useCase 에 전달하고 200 을 반환한다") {
                val capturedRequest = slot<Request>()
                every { useCase.perform(capture(capturedRequest)) } returns
                    PageResult(
                        items = emptyList(),
                        page = 0,
                        size = DEFAULT_SIZE,
                        totalElements = 0L
                    )

                controller
                    .`when`(get("/v1/pages/{pageId}/tags", 10).withAuth())
                    .then(
                        status().isOk,
                        jsonPath("$.items.length()").value(0),
                        jsonPath("$.totalElements").value(0)
                    )
                capturedRequest.captured.pageRequest.page shouldBe 0
                capturedRequest.captured.pageRequest.size shouldBe DEFAULT_SIZE
            }

            it("Authorization 토큰이 없으면 401 을 반환한다") {
                controller
                    .`when`(get("/v1/pages/{pageId}/tags", 10))
                    .then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("pageId 형식이 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(
                        get("/v1/pages/{pageId}/tags", "not-a-number").withAuth()
                    ).then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("page 가 음수면 400 과 한국어 메시지를 반환한다") {
                controller
                    .`when`(
                        get("/v1/pages/{pageId}/tags", 10)
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
