package com.crispinlab.composition.adapter.web.comment

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.composition.application.port.incoming.comment.CommentListingComposition
import com.crispinlab.composition.application.port.incoming.comment.CommentListingComposition.Result
import com.crispinlab.composition.testsupport.CompositionAppControllerDescribeSpec
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.user.domain.user.UserId
import com.crispinlab.user.testsupport.withAuth
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class CommentListingCompositionControllerTest :
    CompositionAppControllerDescribeSpec(tag = "Comment", body = {
        val useCase = mockk<CommentListingComposition>()
        val controller = CommentListingCompositionController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("댓글 목록 조회") {
            it("정상 응답 시 200 과 페이지를 반환한다") {
                every { useCase.perform(any()) } returns
                    PageResult(
                        items =
                            listOf(
                                Result(
                                    commentId = CommentId(1L),
                                    pageId = PageId(10L),
                                    authorId = UserId(100L),
                                    authorHandle = "alice",
                                    content = "첫 댓글",
                                    canEdit = true,
                                    createdAt = DUMMY_INSTANT,
                                    updatedAt = DUMMY_INSTANT
                                ),
                                Result(
                                    commentId = CommentId(2L),
                                    pageId = PageId(10L),
                                    authorId = UserId(101L),
                                    authorHandle = "bob",
                                    content = "두 번째",
                                    canEdit = false,
                                    createdAt = DUMMY_INSTANT,
                                    updatedAt = DUMMY_INSTANT
                                )
                            ),
                        page = 0,
                        size = 20,
                        totalElements = 2L
                    )

                controller
                    .`when`(
                        get("/v1/pages/{pageId}/comments", 10)
                            .withAuth()
                            .param("page", "0")
                            .param("size", "20")
                    ).then(
                        status().isOk,
                        jsonPath("$.items.length()").value(2),
                        jsonPath("$.items[0].commentId").value("1"),
                        jsonPath("$.items[0].content").value("첫 댓글"),
                        jsonPath("$.items[0].authorHandle").value("alice"),
                        jsonPath("$.items[0].canEdit").value(true),
                        jsonPath("$.items[1].authorHandle").value("bob"),
                        jsonPath("$.items[1].canEdit").value(false),
                        jsonPath("$.totalElements").value(2),
                        jsonPath("$.hasNext").value(false)
                    ).document(
                        authHeader(required = true),
                        pagingParameters(),
                        responseFields {
                            "items".array("댓글 목록") {
                                "commentId".string("댓글 식별자")
                                "pageId".string("소속 페이지 식별자")
                                "authorId".string("작성자 식별자")
                                "authorHandle".string(
                                    "작성자 사용자 이름 (삭제된 사용자의 경우 빈 문자열)"
                                )
                                "content".string("본문 (TipTap JSON)")
                                "canEdit".boolean(
                                    "현재 viewer 가 이 댓글을 수정할 수 있는지. " +
                                        "ADMIN 글로벌 권한 또는 (author 본인 && 스페이스 쓰기 권한) 일 때 true."
                                )
                                "createdAt".datetime("생성 시각")
                                "updatedAt".datetime("최근 갱신 시각")
                            }
                            "page".number("현재 페이지")
                            "size".number("페이지당 항목 수")
                            "totalElements".number("총 항목 수")
                            "totalPages".number("총 페이지 수")
                            "hasNext".boolean("다음 페이지 존재 여부")
                            "isEmpty".boolean("결과 비어 있음 여부")
                        },
                        responseSchema = "CommentListResponse"
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
                    .`when`(get("/v1/pages/{pageId}/comments", 10).withAuth())
                    .then(
                        status().isOk,
                        jsonPath("$.items.length()").value(0),
                        jsonPath("$.totalElements").value(0)
                    )
            }

            it("Authorization 토큰이 없으면 401 을 반환한다") {
                controller
                    .`when`(get("/v1/pages/{pageId}/comments", 10))
                    .then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("pageId 형식이 숫자가 아니면 UseCase 가 던진 IllegalArgumentException 이 400 으로 매핑된다") {
                every { useCase.perform(any()) } throws
                    IllegalArgumentException("페이지 ID 형식이 올바르지 않습니다.")

                controller
                    .`when`(
                        get("/v1/pages/{pageId}/comments", "not-a-number").withAuth()
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST"),
                        jsonPath("$.message").value("페이지 ID 형식이 올바르지 않습니다.")
                    )
            }

            it("page 가 음수면 UseCase 가 던진 IllegalArgumentException 이 400 으로 매핑된다") {
                every { useCase.perform(any()) } throws
                    IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.")

                controller
                    .`when`(
                        get("/v1/pages/{pageId}/comments", 10)
                            .withAuth()
                            .param("page", "-1")
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST"),
                        jsonPath("$.message").value("페이지 번호는 0 이상이어야 합니다.")
                    )
            }
        }
    })
