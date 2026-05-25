package com.crispinlab.space.adapter.web.comment

import com.crispinlab.space.application.port.incoming.comment.CommentDeleting
import com.crispinlab.space.testsupport.SpaceAppControllerDescribeSpec
import com.crispinlab.user.testsupport.withAuth
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class CommentDeletingControllerTest :
    SpaceAppControllerDescribeSpec(tag = "Comment", body = {
        val useCase = mockk<CommentDeleting>()
        val controller = CommentDeletingController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("댓글 삭제") {
            it("성공하면 204 를 반환한다") {
                every {
                    useCase.perform(
                        match {
                            it.pageId.value == 10L &&
                                it.commentId.value == 7L &&
                                it.viewer.userId.value == 100L
                        }
                    )
                } just runs

                controller
                    .`when`(
                        delete("/v1/pages/{pageId}/comments/{commentId}", 10, 7).withAuth()
                    ).then(status().isNoContent)
                    .document(authHeaderRequired())

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

            it("Authorization 토큰이 없으면 401 을 반환한다") {
                controller
                    .`when`(delete("/v1/pages/{pageId}/comments/{commentId}", 10, 7))
                    .then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("commentId 형식이 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(
                        delete("/v1/pages/{pageId}/comments/{commentId}", 10, "not-a-number")
                            .withAuth()
                    ).then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
