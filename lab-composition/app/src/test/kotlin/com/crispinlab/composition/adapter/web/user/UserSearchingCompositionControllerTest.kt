package com.crispinlab.composition.adapter.web.user

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.composition.application.port.incoming.user.UserSearchingComposition
import com.crispinlab.composition.application.port.incoming.user.UserSearchingComposition.Request
import com.crispinlab.composition.application.port.incoming.user.UserSearchingComposition.Result
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

class UserSearchingCompositionControllerTest :
    CompositionAppControllerDescribeSpec(tag = "User", body = {
        val useCase = mockk<UserSearchingComposition>()
        val controller = UserSearchingCompositionController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("사용자 검색") {
            it("정상 검색 시 200 과 매칭 결과 + 소속 스페이스 집합 + alreadyMember 를 반환한다") {
                every { useCase.perform(any()) } returns
                    Result(
                        items =
                            listOf(
                                Result.Item(
                                    userId = UserId(1L),
                                    handle = Handle("alice"),
                                    memberOfSpaceIds = listOf(SpaceId(10L), SpaceId(20L)),
                                    alreadyMember = true
                                ),
                                Result.Item(
                                    userId = UserId(2L),
                                    handle = Handle("alice_kim"),
                                    memberOfSpaceIds = listOf(SpaceId(20L)),
                                    alreadyMember = false
                                )
                            )
                    )

                controller
                    .`when`(
                        get("/v1/users")
                            .withAuth()
                            .param("query", "ali")
                            .param("size", "10")
                            .param("spaceId", "10")
                    ).then(
                        status().isOk,
                        jsonPath("$.items.length()").value(2),
                        jsonPath("$.items[0].userId").value("1"),
                        jsonPath("$.items[0].handle").value("alice"),
                        jsonPath("$.items[0].memberOfSpaceIds.length()").value(2),
                        jsonPath("$.items[0].memberOfSpaceIds[0]").value("10"),
                        jsonPath("$.items[0].memberOfSpaceIds[1]").value("20"),
                        jsonPath("$.items[0].alreadyMember").value(true),
                        jsonPath("$.items[1].userId").value("2"),
                        jsonPath("$.items[1].handle").value("alice_kim"),
                        jsonPath("$.items[1].memberOfSpaceIds.length()").value(1),
                        jsonPath("$.items[1].memberOfSpaceIds[0]").value("20"),
                        jsonPath("$.items[1].alreadyMember").value(false)
                    ).document(
                        authHeader(required = true),
                        queryParameters(
                            "query" isParameterFor "검색어 (handle 부분 일치, 대소문자 무시, 1~30자)",
                            "size" isParameterFor
                                "결과 수 (1 ~ 20, 기본값 10)" isOptional true,
                            "spaceId" isParameterFor
                                "초대 대상 스페이스 식별자. 지정 시 각 item 의 alreadyMember 가 " +
                                "true/false 로 채워진다. 지정 시 Long 형식이어야 하며, 빈 값이면 400." isOptional true
                        ),
                        responseFields {
                            "items".array("매칭된 사용자 목록") {
                                "userId".string("사용자 식별자")
                                "handle".string("사용자 이름")
                                "memberOfSpaceIds".array(
                                    "사용자가 소속된 스페이스 식별자 목록 " +
                                        "(검색자가 볼 수 있는 스페이스만 노출, SpaceId 오름차순, 없으면 빈 배열)"
                                )
                                "alreadyMember".boolean(
                                    "요청 spaceId 에 이미 참여 중인지 여부. " +
                                        "spaceId 미지정 또는 검색자가 해당 스페이스를 볼 수 없으면 null.",
                                    optional = true
                                )
                            }
                        },
                        responseSchema = "UserSearchResponse"
                    )
            }

            it("spaceId 를 Request 에 그대로 전달한다") {
                val captured = slot<Request>()
                every { useCase.perform(capture(captured)) } returns Result(items = emptyList())

                controller
                    .`when`(
                        get("/v1/users")
                            .withAuth()
                            .param("query", "ali")
                            .param("spaceId", "42")
                    ).then(status().isOk)

                captured.captured.spaceId shouldBe SpaceId(42L)
            }

            it("spaceId 를 생략하면 Request.spaceId 는 null 이다") {
                val captured = slot<Request>()
                every { useCase.perform(capture(captured)) } returns Result(items = emptyList())

                controller
                    .`when`(
                        get("/v1/users")
                            .withAuth()
                            .param("query", "ali")
                    ).then(status().isOk)

                captured.captured.spaceId shouldBe null
            }

            it("spaceId 가 Long 형식이 아니면 400 을 반환한다") {
                controller
                    .`when`(
                        get("/v1/users")
                            .withAuth()
                            .param("query", "ali")
                            .param("spaceId", "not-a-long")
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("spaceId 가 빈 문자열이면 400 을 반환한다") {
                controller
                    .`when`(
                        get("/v1/users")
                            .withAuth()
                            .param("query", "ali")
                            .param("spaceId", "")
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("size 를 생략하면 default 10 으로 Request 가 만들어진다") {
                val captured = slot<Request>()
                every { useCase.perform(capture(captured)) } returns Result(items = emptyList())

                controller
                    .`when`(
                        get("/v1/users")
                            .withAuth()
                            .param("query", "bob")
                    ).then(status().isOk)

                captured.captured.query shouldBe "bob"
                captured.captured.size shouldBe 10
            }

            it("검색자의 Viewer.Member 가 Request 에 담긴다") {
                val requestSlot = slot<Request>()
                every { useCase.perform(capture(requestSlot)) } returns Result(items = emptyList())

                controller
                    .`when`(
                        get("/v1/users")
                            .withAuth(userId = "500", role = SystemRole.USER)
                            .param("query", "ali")
                    ).then(status().isOk)

                requestSlot.captured.viewer.userId shouldBe UserId(500L)
                requestSlot.captured.viewer.isAdmin shouldBe false
            }

            it("ADMIN 검색자는 isAdmin=true 로 Viewer.Member 가 담긴다") {
                val requestSlot = slot<Request>()
                every { useCase.perform(capture(requestSlot)) } returns Result(items = emptyList())

                controller
                    .`when`(
                        get("/v1/users")
                            .withAuth(userId = "500", role = SystemRole.ADMIN)
                            .param("query", "ali")
                    ).then(status().isOk)

                requestSlot.captured.viewer.isAdmin shouldBe true
            }

            it("검색 결과가 비면 빈 items 를 반환한다") {
                every { useCase.perform(any()) } returns Result(items = emptyList())

                controller
                    .`when`(
                        get("/v1/users")
                            .withAuth()
                            .param("query", "none")
                    ).then(
                        status().isOk,
                        jsonPath("$.items.length()").value(0)
                    )
            }

            it("query 가 누락되면 400 을 반환하고 use case 를 호출하지 않는다") {
                controller
                    .`when`(get("/v1/users").withAuth())
                    .then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("query 가 빈 문자열이면 UseCase 가 던진 IllegalArgumentException 이 400 으로 매핑된다") {
                every { useCase.perform(any()) } throws
                    IllegalArgumentException("검색어는 1자 이상 30자 이하여야 합니다.")

                controller
                    .`when`(
                        get("/v1/users")
                            .withAuth()
                            .param("query", "")
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST")
                    )
            }

            it("size 가 21 이상이면 UseCase 가 던진 IllegalArgumentException 이 400 으로 매핑된다") {
                every { useCase.perform(any()) } throws
                    IllegalArgumentException("결과 수는 1 이상 20 이하여야 합니다.")

                controller
                    .`when`(
                        get("/v1/users")
                            .withAuth()
                            .param("query", "ok")
                            .param("size", "21")
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST")
                    )
            }

            it("Authorization 헤더가 없으면 401 을 반환하고 use case 를 호출하지 않는다") {
                controller
                    .`when`(
                        get("/v1/users").param("query", "ali")
                    ).then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
