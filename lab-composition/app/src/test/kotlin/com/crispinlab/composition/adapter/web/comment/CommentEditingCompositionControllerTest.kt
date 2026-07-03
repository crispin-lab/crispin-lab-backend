package com.crispinlab.composition.adapter.web.comment

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.requestFields
import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.composition.application.port.incoming.comment.CommentEditingComposition
import com.crispinlab.composition.application.port.incoming.comment.CommentEditingComposition.Result
import com.crispinlab.composition.testsupport.CompositionAppControllerDescribeSpec
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.user.testsupport.withAuth
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class CommentEditingCompositionControllerTest :
    CompositionAppControllerDescribeSpec(tag = "Comment", body = {
        val useCase = mockk<CommentEditingComposition>()
        val controller = CommentEditingCompositionController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("댓글 수정") {
            it("본문 변경 시 200 과 갱신 결과를 반환한다") {
                every {
                    useCase.perform(
                        match {
                            it.pageId == "10" &&
                                it.commentId == "7" &&
                                it.content == "수정된 댓글" &&
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

            it("commentId 형식이 숫자가 아니면 UseCase 가 던진 IllegalArgumentException 이 400 으로 매핑된다") {
                every { useCase.perform(any()) } throws
                    IllegalArgumentException("댓글 ID 형식이 올바르지 않습니다.")

                controller
                    .`when`(
                        put("/v1/pages/{pageId}/comments/{commentId}", 10, "not-a-number")
                            .withAuth()
                            .body(mapOf("content" to "수정된 댓글"))
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST"),
                        jsonPath("$.message").value("댓글 ID 형식이 올바르지 않습니다.")
                    )
            }
        }
    })
