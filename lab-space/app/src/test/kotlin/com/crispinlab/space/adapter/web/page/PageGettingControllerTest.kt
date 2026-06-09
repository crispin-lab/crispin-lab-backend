package com.crispinlab.space.adapter.web.page

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.space.application.port.incoming.page.PageGetting
import com.crispinlab.space.application.port.incoming.page.PageGetting.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageErrorCode
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.SpaceAppControllerDescribeSpec
import com.crispinlab.user.domain.user.UserId
import com.crispinlab.user.testsupport.withAuth
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class PageGettingControllerTest :
    SpaceAppControllerDescribeSpec(tag = "Page", body = {
        val useCase = mockk<PageGetting>()
        val controller = PageGettingController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("페이지 단건 조회") {
            it("존재하면 200 과 정보를 반환한다") {
                every { useCase.perform(any()) } returns
                    Result(
                        pageId = PageId(3L),
                        spaceId = SpaceId(10L),
                        parentPageId = PageId(2L),
                        authorId = UserId(100L),
                        title = "오늘의 회고",
                        content = "본문",
                        visibility = "DRAFT",
                        currentVersion = 1,
                        createdAt = DUMMY_INSTANT,
                        updatedAt = DUMMY_INSTANT,
                        ancestors =
                            listOf(
                                Result.AncestorSummary(
                                    pageId = PageId(1L),
                                    title = "개인 노트"
                                ),
                                Result.AncestorSummary(
                                    pageId = PageId(2L),
                                    title = "아이디어"
                                )
                            )
                    )

                controller
                    .`when`(
                        get("/v1/pages/{pageId}", 3).withAuth()
                    ).then(
                        status().isOk,
                        jsonPath("$.pageId").value("3"),
                        jsonPath("$.title").value("오늘의 회고"),
                        jsonPath("$.visibility").value("DRAFT"),
                        jsonPath("$.ancestors[0].pageId").value("1"),
                        jsonPath("$.ancestors[0].title").value("개인 노트"),
                        jsonPath("$.ancestors[1].pageId").value("2"),
                        jsonPath("$.ancestors[1].title").value("아이디어")
                    ).document(
                        authHeaderRequired(),
                        responseFields {
                            "pageId".string("페이지 식별자")
                            "spaceId".string("소속 스페이스 식별자")
                            "parentPageId".string("부모 페이지 식별자", optional = true)
                            "authorId".string("작성자 식별자")
                            "title".string("제목")
                            "content".string("본문")
                            "visibility".string("공개 범위")
                            "currentVersion".number("현재 버전")
                            "createdAt".datetime("생성 시각")
                            "updatedAt".datetime("최근 갱신 시각")
                            "ancestors".array("조상 페이지 목록 — root → 직계 부모 순서") {
                                "pageId".string("조상 페이지 식별자")
                                "title".string("조상 페이지 제목")
                            }
                        },
                        responseSchema = "PageGetResponse"
                    )
            }

            it("없으면 404 를 반환한다") {
                every { useCase.perform(any()) } throws
                    NotFoundException(PageErrorCode.PAGE_NOT_FOUND)

                controller
                    .`when`(
                        get("/v1/pages/{pageId}", 999).withAuth()
                    ).then(
                        status().isNotFound,
                        jsonPath("$.code").value("PAGE_NOT_FOUND"),
                        jsonPath("$.message").value("페이지를 찾을 수 없습니다.")
                    )
            }

            it("pageId 형식이 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(get("/v1/pages/{pageId}", "not-a-number").withAuth())
                    .then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("비로그인 상태에서도 PUBLIC 페이지는 200 으로 응답한다") {
                every { useCase.perform(any()) } returns
                    Result(
                        pageId = PageId(1L),
                        spaceId = SpaceId(10L),
                        parentPageId = null,
                        authorId = UserId(100L),
                        title = "공개 페이지",
                        content = "본문",
                        visibility = "PUBLIC",
                        currentVersion = 1,
                        createdAt = DUMMY_INSTANT,
                        updatedAt = DUMMY_INSTANT,
                        ancestors = emptyList()
                    )

                controller
                    .`when`(get("/v1/pages/{pageId}", 1))
                    .then(
                        status().isOk,
                        jsonPath("$.visibility").value("PUBLIC")
                    )
                verify {
                    useCase.perform(
                        match { it.viewer == Viewer.Anonymous }
                    )
                }
            }
        }
    })
