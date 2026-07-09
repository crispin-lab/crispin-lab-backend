package com.crispinlab.composition.adapter.web.mention

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.composition.application.port.incoming.mention.MentionSuggesting
import com.crispinlab.composition.application.port.incoming.mention.MentionSuggesting.Request
import com.crispinlab.composition.application.port.incoming.mention.MentionSuggesting.Result
import com.crispinlab.composition.testsupport.CompositionAppControllerDescribeSpec
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.SystemRole
import com.crispinlab.user.domain.user.UserId
import com.crispinlab.user.testsupport.withAuth
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class MentionSuggestingCompositionControllerTest :
    CompositionAppControllerDescribeSpec(tag = "Mention", body = {
        val useCase = mockk<MentionSuggesting>()
        val controller = MentionSuggestingCompositionController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("mention 후보 조회") {
            it("정상 조회 시 200 과 볼 수 있는 사용자 목록을 반환한다") {
                every { useCase.perform(any()) } returns
                    Result(
                        items =
                            listOf(
                                Result.Item(userId = UserId(1L), handle = Handle("alice")),
                                Result.Item(userId = UserId(2L), handle = Handle("alice_kim"))
                            )
                    )

                controller
                    .`when`(
                        get("/v1/mention-candidates")
                            .withAuth()
                            .param("query", "ali")
                            .param("size", "5")
                            .param("spaceId", "10")
                            .param("spaceVisibility", "PUBLIC")
                            .param("pageVisibility", "MEMBER")
                            .param("pageAuthorId", "100")
                    ).then(
                        status().isOk,
                        jsonPath("$.items.length()").value(2),
                        jsonPath("$.items[0].userId").value("1"),
                        jsonPath("$.items[0].handle").value("alice"),
                        jsonPath("$.items[1].userId").value("2"),
                        jsonPath("$.items[1].handle").value("alice_kim")
                    ).document(
                        authHeader(required = true),
                        queryParameters(
                            "query" isParameterFor "검색어 (handle 부분 일치, 대소문자 무시, 1~30자)",
                            "size" isParameterFor
                                "결과 수 (1 ~ 20, 기본값 10)" isOptional true,
                            "spaceId" isParameterFor
                                "편집 중인 페이지가 속한 스페이스 식별자",
                            "spaceVisibility" isParameterFor
                                "스페이스 공개 범위 (INTERNAL, PUBLIC). 편집 중 미저장 상태를 반영하기 위해 클라이언트가 명시.",
                            "pageVisibility" isParameterFor
                                "페이지 공개 범위 (DRAFT, INTERNAL, MEMBER, PUBLIC). 편집 중 미저장 상태를 반영하기 위해 클라이언트가 명시.",
                            "pageAuthorId" isParameterFor
                                "편집 중인 페이지 작성자 식별자"
                        ),
                        responseFields {
                            "items".array("mention 가능한 사용자 목록") {
                                "userId".string("사용자 식별자")
                                "handle".string("사용자 이름")
                            }
                        },
                        responseSchema = "MentionCandidateGetResponse"
                    )
            }

            it("size 를 생략하면 default 10 으로 Request 가 만들어진다") {
                val captured = slot<Request>()
                every { useCase.perform(capture(captured)) } returns Result(items = emptyList())

                controller
                    .`when`(
                        get("/v1/mention-candidates")
                            .withAuth()
                            .param("query", "ali")
                            .param("spaceId", "10")
                            .param("spaceVisibility", "PUBLIC")
                            .param("pageVisibility", "PUBLIC")
                            .param("pageAuthorId", "100")
                    ).then(status().isOk)

                captured.captured.size shouldBe 10
            }

            it("Request 에 spaceId / pageAuthorId 가 도메인 타입으로 변환되어 담긴다") {
                val captured = slot<Request>()
                every { useCase.perform(capture(captured)) } returns Result(items = emptyList())

                controller
                    .`when`(
                        get("/v1/mention-candidates")
                            .withAuth()
                            .param("query", "ali")
                            .param("spaceId", "42")
                            .param("spaceVisibility", "INTERNAL")
                            .param("pageVisibility", "DRAFT")
                            .param("pageAuthorId", "500")
                    ).then(status().isOk)

                captured.captured.spaceId shouldBe SpaceId(42L)
                captured.captured.pageAuthorId shouldBe UserId(500L)
            }

            it("검색자의 Viewer.Member 가 Request 에 담긴다 (USER)") {
                val captured = slot<Request>()
                every { useCase.perform(capture(captured)) } returns Result(items = emptyList())

                controller
                    .`when`(
                        get("/v1/mention-candidates")
                            .withAuth(userId = "500", role = SystemRole.USER)
                            .param("query", "ali")
                            .param("spaceId", "10")
                            .param("spaceVisibility", "PUBLIC")
                            .param("pageVisibility", "PUBLIC")
                            .param("pageAuthorId", "100")
                    ).then(status().isOk)

                captured.captured.viewer.userId shouldBe UserId(500L)
                captured.captured.viewer.isAdmin shouldBe false
            }

            it("ADMIN 검색자는 isAdmin=true 로 Viewer.Member 가 담긴다") {
                val captured = slot<Request>()
                every { useCase.perform(capture(captured)) } returns Result(items = emptyList())

                controller
                    .`when`(
                        get("/v1/mention-candidates")
                            .withAuth(userId = "500", role = SystemRole.ADMIN)
                            .param("query", "ali")
                            .param("spaceId", "10")
                            .param("spaceVisibility", "PUBLIC")
                            .param("pageVisibility", "PUBLIC")
                            .param("pageAuthorId", "100")
                    ).then(status().isOk)

                captured.captured.viewer.isAdmin shouldBe true
            }

            it("검색 결과가 비면 빈 items 를 반환한다") {
                every { useCase.perform(any()) } returns Result(items = emptyList())

                controller
                    .`when`(
                        get("/v1/mention-candidates")
                            .withAuth()
                            .param("query", "none")
                            .param("spaceId", "10")
                            .param("spaceVisibility", "PUBLIC")
                            .param("pageVisibility", "PUBLIC")
                            .param("pageAuthorId", "100")
                    ).then(
                        status().isOk,
                        jsonPath("$.items.length()").value(0)
                    )
            }

            it("query 가 누락되면 400 을 반환하고 UseCase 를 호출하지 않는다") {
                controller
                    .`when`(
                        get("/v1/mention-candidates")
                            .withAuth()
                            .param("spaceId", "10")
                            .param("spaceVisibility", "PUBLIC")
                            .param("pageVisibility", "PUBLIC")
                            .param("pageAuthorId", "100")
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("spaceId 가 누락되면 400 을 반환한다") {
                controller
                    .`when`(
                        get("/v1/mention-candidates")
                            .withAuth()
                            .param("query", "ali")
                            .param("spaceVisibility", "PUBLIC")
                            .param("pageVisibility", "PUBLIC")
                            .param("pageAuthorId", "100")
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("spaceVisibility 가 누락되면 400 을 반환한다") {
                controller
                    .`when`(
                        get("/v1/mention-candidates")
                            .withAuth()
                            .param("query", "ali")
                            .param("spaceId", "10")
                            .param("pageVisibility", "PUBLIC")
                            .param("pageAuthorId", "100")
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("pageVisibility 가 누락되면 400 을 반환한다") {
                controller
                    .`when`(
                        get("/v1/mention-candidates")
                            .withAuth()
                            .param("query", "ali")
                            .param("spaceId", "10")
                            .param("spaceVisibility", "PUBLIC")
                            .param("pageAuthorId", "100")
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("pageAuthorId 가 누락되면 400 을 반환한다") {
                controller
                    .`when`(
                        get("/v1/mention-candidates")
                            .withAuth()
                            .param("query", "ali")
                            .param("spaceId", "10")
                            .param("spaceVisibility", "PUBLIC")
                            .param("pageVisibility", "PUBLIC")
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("spaceId 가 Long 형식이 아니면 400 을 반환한다") {
                controller
                    .`when`(
                        get("/v1/mention-candidates")
                            .withAuth()
                            .param("query", "ali")
                            .param("spaceId", "not-a-long")
                            .param("spaceVisibility", "PUBLIC")
                            .param("pageVisibility", "PUBLIC")
                            .param("pageAuthorId", "100")
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("spaceVisibility 가 지원되지 않는 값이면 400 을 반환한다") {
                controller
                    .`when`(
                        get("/v1/mention-candidates")
                            .withAuth()
                            .param("query", "ali")
                            .param("spaceId", "10")
                            .param("spaceVisibility", "UNKNOWN")
                            .param("pageVisibility", "PUBLIC")
                            .param("pageAuthorId", "100")
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("pageVisibility 가 지원되지 않는 값이면 400 을 반환한다") {
                controller
                    .`when`(
                        get("/v1/mention-candidates")
                            .withAuth()
                            .param("query", "ali")
                            .param("spaceId", "10")
                            .param("spaceVisibility", "PUBLIC")
                            .param("pageVisibility", "UNKNOWN")
                            .param("pageAuthorId", "100")
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("size 가 21 이상이면 UseCase 가 던진 IllegalArgumentException 이 400 으로 매핑된다") {
                every { useCase.perform(any()) } throws
                    IllegalArgumentException("결과 수는 1 이상 20 이하여야 합니다.")

                controller
                    .`when`(
                        get("/v1/mention-candidates")
                            .withAuth()
                            .param("query", "ali")
                            .param("size", "21")
                            .param("spaceId", "10")
                            .param("spaceVisibility", "PUBLIC")
                            .param("pageVisibility", "PUBLIC")
                            .param("pageAuthorId", "100")
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST")
                    )
            }

            it("Authorization 헤더가 없으면 401 을 반환하고 UseCase 를 호출하지 않는다") {
                controller
                    .`when`(
                        get("/v1/mention-candidates")
                            .param("query", "ali")
                            .param("spaceId", "10")
                            .param("spaceVisibility", "PUBLIC")
                            .param("pageVisibility", "PUBLIC")
                            .param("pageAuthorId", "100")
                    ).then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
