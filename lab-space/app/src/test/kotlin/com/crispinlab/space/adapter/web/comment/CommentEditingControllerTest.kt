package com.crispinlab.space.adapter.web.comment

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.requestFields
import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.space.application.port.incoming.comment.CommentEditing
import com.crispinlab.space.application.port.incoming.comment.CommentEditing.Result
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.SpaceAppControllerDescribeSpec
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
                                it.body == "수정된 댓글" &&
                                it.currentUserId.value == 100L
                        }
                    )
                } returns
                    Result(
                        commentId = "7",
                        body = "수정된 댓글",
                        updatedAt = DUMMY_INSTANT
                    )

                controller
                    .`when`(
                        put("/v1/pages/{pageId}/comments/{commentId}", 10, 7)
                            .withUserHeader()
                            .body(mapOf("body" to "수정된 댓글"))
                    ).then(
                        status().isOk,
                        jsonPath("$.commentId").value("7"),
                        jsonPath("$.body").value("수정된 댓글")
                    ).document(
                        userHeaderRequired(),
                        requestFields {
                            "body".string("수정된 본문")
                        },
                        responseFields {
                            "commentId".string("댓글 식별자")
                            "body".string("갱신된 본문")
                            "updatedAt".datetime("갱신 시각")
                        }
                    )

                verify(exactly = 1) {
                    useCase.perform(
                        match {
                            it.pageId.value == 10L &&
                                it.commentId.value == 7L &&
                                it.body == "수정된 댓글" &&
                                it.currentUserId.value == 100L
                        }
                    )
                }
            }

            it("X-User-Id 헤더가 없으면 400 을 반환한다") {
                controller
                    .`when`(
                        put("/v1/pages/{pageId}/comments/{commentId}", 10, 7)
                            .body(mapOf("body" to "수정된 댓글"))
                    ).then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("commentId 형식이 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(
                        put("/v1/pages/{pageId}/comments/{commentId}", 10, "not-a-number")
                            .withUserHeader()
                            .body(mapOf("body" to "수정된 댓글"))
                    ).then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
