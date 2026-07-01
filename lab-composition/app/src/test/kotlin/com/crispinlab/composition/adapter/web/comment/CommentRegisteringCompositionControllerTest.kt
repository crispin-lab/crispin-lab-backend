package com.crispinlab.composition.adapter.web.comment

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.requestFields
import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.composition.application.port.outgoing.user.UserHandleLookup
import com.crispinlab.composition.testsupport.CompositionAppControllerDescribeSpec
import com.crispinlab.space.application.port.incoming.comment.CommentRegistering
import com.crispinlab.space.application.port.incoming.comment.CommentRegistering.Result
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.user.domain.user.UserId
import com.crispinlab.user.testsupport.withAuth
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class CommentRegisteringCompositionControllerTest :
    CompositionAppControllerDescribeSpec(tag = "Comment", body = {
        val useCase = mockk<CommentRegistering>()
        val userHandleLookup = mockk<UserHandleLookup>()
        val controller = CommentRegisteringCompositionController(useCase, userHandleLookup)

        beforeEach {
            clearMocks(useCase, userHandleLookup)
            every { userHandleLookup.handlesOf(any()) } returns
                mapOf(UserId(100L) to "test_user")
        }

        describe("댓글 등록") {
            it("정상 생성 시 201 과 commentId 를 반환한다") {
                every {
                    useCase.perform(
                        match {
                            it.pageId.value == 10L &&
                                it.content.raw == "첫 댓글" &&
                                it.viewer.userId.value == 100L
                        }
                    )
                } returns
                    Result(
                        commentId = CommentId(42L),
                        authorId = UserId(100L)
                    )

                controller
                    .`when`(
                        post("/v1/pages/{pageId}/comments", 10)
                            .withAuth()
                            .body(mapOf("content" to "첫 댓글"))
                    ).then(
                        status().isCreated,
                        jsonPath("$.commentId").value("42"),
                        jsonPath("$.authorHandle").value("test_user")
                    ).document(
                        authHeader(required = true),
                        requestFields {
                            "content".string("댓글 본문 (TipTap JSON)")
                        },
                        responseFields {
                            "commentId".string("생성된 댓글 식별자")
                            "authorHandle".string(
                                "작성자 사용자 이름 (삭제된 사용자의 경우 빈 문자열)"
                            )
                        },
                        requestSchema = "CommentRegisterRequest",
                        responseSchema = "CommentRegisterResponse"
                    )
            }

            it("handle 조회가 실패해도 쓰기 성공 응답을 반환한다 (authorHandle 빈 문자열)") {
                every { userHandleLookup.handlesOf(any()) } throws
                    RuntimeException("lookup failure")
                every { useCase.perform(any()) } returns
                    Result(
                        commentId = CommentId(42L),
                        authorId = UserId(100L)
                    )

                controller
                    .`when`(
                        post("/v1/pages/{pageId}/comments", 10)
                            .withAuth()
                            .body(mapOf("content" to "첫 댓글"))
                    ).then(
                        status().isCreated,
                        jsonPath("$.commentId").value("42"),
                        jsonPath("$.authorHandle").value("")
                    )
            }

            it("Authorization 토큰이 없으면 401 을 반환한다") {
                controller
                    .`when`(
                        post("/v1/pages/{pageId}/comments", 10)
                            .body(mapOf("content" to "첫 댓글"))
                    ).then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("pageId 형식이 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(
                        post("/v1/pages/{pageId}/comments", "not-a-number")
                            .withAuth()
                            .body(mapOf("content" to "첫 댓글"))
                    ).then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
