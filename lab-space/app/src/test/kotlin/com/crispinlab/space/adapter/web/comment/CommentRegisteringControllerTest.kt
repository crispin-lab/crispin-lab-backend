package com.crispinlab.space.adapter.web.comment

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.requestFields
import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.space.application.port.incoming.comment.CommentRegistering
import com.crispinlab.space.application.port.incoming.comment.CommentRegistering.Result
import com.crispinlab.space.testsupport.SpaceAppControllerDescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class CommentRegisteringControllerTest :
    SpaceAppControllerDescribeSpec(tag = "Comment", body = {
        val useCase = mockk<CommentRegistering>()
        val controller = CommentRegisteringController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("댓글 등록") {
            it("정상 생성 시 201 과 commentId 를 반환한다") {
                every {
                    useCase.perform(
                        match {
                            it.pageId.value == 10L &&
                                it.body == "첫 댓글" &&
                                it.currentUserId.value == 100L
                        }
                    )
                } returns Result(commentId = "42")

                controller
                    .`when`(
                        post("/v1/pages/{pageId}/comments", 10)
                            .withUserHeader()
                            .body(mapOf("body" to "첫 댓글"))
                    ).then(
                        status().isCreated,
                        jsonPath("$.commentId").value("42")
                    ).document(
                        userHeaderRequired(),
                        requestFields {
                            "body".string("댓글 본문")
                        },
                        responseFields {
                            "commentId".string("생성된 댓글 식별자")
                        }
                    )
            }

            it("X-User-Id 헤더가 없으면 400 을 반환한다") {
                controller
                    .`when`(
                        post("/v1/pages/{pageId}/comments", 10)
                            .body(mapOf("body" to "첫 댓글"))
                    ).then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("pageId 형식이 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(
                        post("/v1/pages/{pageId}/comments", "not-a-number")
                            .withUserHeader()
                            .body(mapOf("body" to "첫 댓글"))
                    ).then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
