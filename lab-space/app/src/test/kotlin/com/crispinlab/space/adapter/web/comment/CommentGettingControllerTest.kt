package com.crispinlab.space.adapter.web.comment

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.space.application.port.incoming.comment.CommentGetting
import com.crispinlab.space.application.port.incoming.comment.CommentGetting.Result
import com.crispinlab.space.domain.comment.CommentErrorCode
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.domain.page.PageId
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

class CommentGettingControllerTest :
    SpaceAppControllerDescribeSpec(tag = "Comment", body = {
        val useCase = mockk<CommentGetting>()
        val controller = CommentGettingController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("댓글 단건 조회") {
            it("존재하면 200 과 정보를 반환한다") {
                every {
                    useCase.perform(
                        match {
                            it.pageId.value == 10L &&
                                it.commentId.value == 7L &&
                                it.viewer.userId.value == 100L
                        }
                    )
                } returns
                    Result(
                        commentId = CommentId(7L),
                        pageId = PageId(10L),
                        authorId = UserId(100L),
                        authorHandle = "test_user",
                        body = "안녕하세요",
                        createdAt = DUMMY_INSTANT,
                        updatedAt = DUMMY_INSTANT
                    )

                controller
                    .`when`(
                        get("/v1/pages/{pageId}/comments/{commentId}", 10, 7).withAuth()
                    ).then(
                        status().isOk,
                        jsonPath("$.commentId").value("7"),
                        jsonPath("$.body").value("안녕하세요"),
                        jsonPath("$.authorHandle").value("test_user")
                    ).document(
                        authHeader(required = true),
                        responseFields {
                            "commentId".string("댓글 식별자")
                            "pageId".string("소속 페이지 식별자")
                            "authorId".string("작성자 식별자")
                            "authorHandle".string(
                                "작성자 사용자 이름 (삭제된 사용자의 경우 빈 문자열)"
                            )
                            "body".string("본문")
                            "createdAt".datetime("생성 시각")
                            "updatedAt".datetime("최근 갱신 시각")
                        },
                        responseSchema = "CommentGetResponse"
                    )

                verify(exactly = 1) {
                    useCase.perform(
                        match {
                            it.pageId.value == 10L &&
                                it.commentId.value == 7L &&
                                it.viewer.userId.value == 100L
                        }
                    )
                }
            }

            it("없으면 404 를 반환한다") {
                every { useCase.perform(any()) } throws
                    NotFoundException(CommentErrorCode.COMMENT_NOT_FOUND)

                controller
                    .`when`(
                        get("/v1/pages/{pageId}/comments/{commentId}", 10, 999).withAuth()
                    ).then(
                        status().isNotFound,
                        jsonPath("$.code").value("COMMENT_NOT_FOUND"),
                        jsonPath("$.message").value("댓글을 찾을 수 없습니다.")
                    )
            }

            it("Authorization 토큰이 없으면 401 을 반환한다") {
                controller
                    .`when`(get("/v1/pages/{pageId}/comments/{commentId}", 10, 1))
                    .then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("commentId 형식이 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(
                        get("/v1/pages/{pageId}/comments/{commentId}", 10, "not-a-number")
                            .withAuth()
                    ).then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
