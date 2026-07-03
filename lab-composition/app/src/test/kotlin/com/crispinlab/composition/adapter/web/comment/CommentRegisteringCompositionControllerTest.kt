package com.crispinlab.composition.adapter.web.comment

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.requestFields
import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.composition.application.port.incoming.comment.CommentRegisteringComposition
import com.crispinlab.composition.application.port.incoming.comment.CommentRegisteringComposition.Result
import com.crispinlab.composition.testsupport.CompositionAppControllerDescribeSpec
import com.crispinlab.space.domain.comment.CommentId
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
        val useCase = mockk<CommentRegisteringComposition>()
        val controller = CommentRegisteringCompositionController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("댓글 등록") {
            it("정상 생성 시 201 과 commentId 를 반환한다") {
                every {
                    useCase.perform(
                        match {
                            it.pageId == "10" &&
                                it.content == "첫 댓글" &&
                                it.viewer.userId.value == 100L
                        }
                    )
                } returns
                    Result(
                        commentId = CommentId(42L),
                        authorHandle = "test_user"
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

            it("pageId 형식이 숫자가 아니면 UseCase 가 던진 IllegalArgumentException 이 400 으로 매핑된다") {
                every { useCase.perform(any()) } throws
                    IllegalArgumentException("페이지 ID 형식이 올바르지 않습니다.")

                controller
                    .`when`(
                        post("/v1/pages/{pageId}/comments", "not-a-number")
                            .withAuth()
                            .body(mapOf("content" to "첫 댓글"))
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST"),
                        jsonPath("$.message").value("페이지 ID 형식이 올바르지 않습니다.")
                    )
            }
        }
    })
