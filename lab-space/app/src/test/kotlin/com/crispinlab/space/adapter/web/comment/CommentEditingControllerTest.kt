package com.crispinlab.space.adapter.web.comment

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.requestFields
import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.space.application.port.incoming.comment.CommentEditing
import com.crispinlab.space.application.port.incoming.comment.CommentEditing.Result
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.SpaceAppControllerDescribeSpec
import com.crispinlab.user.testsupport.withAuth
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class CommentEditingControllerTest :
    SpaceAppControllerDescribeSpec(tag = "Comment", body = {
        val useCase = mockk<CommentEditing>()
        val controller = CommentEditingController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("댓글 수정") {
            it("본문 변경 시 200 과 갱신 결과를 반환한다") {
                every {
                    useCase.perform(
                        match {
                            it.pageId.value == 10L &&
                                it.commentId.value == 7L &&
                                it.content.raw == "수정된 댓글" &&
                                it.viewer.userId.value == 100L
                        }
                    )
                } returns
                    Result(
                        commentId = CommentId(7L),
                        authorHandle = "test_user",
                        content = "수정된 댓글",
                        updatedAt = DUMMY_INSTANT
                    )

                controller
                    .`when`(
                        put("/v1/pages/{pageId}/comments/{commentId}", 10, 7)
                            .withAuth()
                            .body(mapOf("content" to "수정된 댓글"))
                    ).then(
                        status().isOk,
                        jsonPath("$.commentId").value("7"),
                        jsonPath("$.content").value("수정된 댓글"),
                        jsonPath("$.authorHandle").value("test_user")
                    ).document(
                        authHeader(required = true),
                        requestFields {
                            "content".string("수정된 본문 (TipTap JSON)")
                        },
                        responseFields {
                            "commentId".string("댓글 식별자")
                            "authorHandle".string(
                                "작성자 사용자 이름 (삭제된 사용자의 경우 빈 문자열)"
                            )
                            "content".string("갱신된 본문 (TipTap JSON)")
                            "updatedAt".datetime("갱신 시각")
                        },
                        requestSchema = "CommentEditRequest",
                        responseSchema = "CommentEditResponse"
                    )

                verify(exactly = 1) {
                    useCase.perform(
                        match {
                            it.pageId.value == 10L &&
                                it.commentId.value == 7L &&
                                it.content.raw == "수정된 댓글" &&
                                it.viewer.userId.value == 100L
                        }
                    )
                }
            }

            it("Authorization 토큰이 없으면 401 을 반환한다") {
                controller
                    .`when`(
                        put("/v1/pages/{pageId}/comments/{commentId}", 10, 7)
                            .body(mapOf("content" to "수정된 댓글"))
                    ).then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("commentId 형식이 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(
                        put("/v1/pages/{pageId}/comments/{commentId}", 10, "not-a-number")
                            .withAuth()
                            .body(mapOf("content" to "수정된 댓글"))
                    ).then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
