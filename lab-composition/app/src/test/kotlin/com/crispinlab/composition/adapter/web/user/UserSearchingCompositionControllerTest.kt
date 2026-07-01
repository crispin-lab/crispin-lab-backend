package com.crispinlab.composition.adapter.web.user

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.composition.application.port.outgoing.space.SpaceMembershipLookup
import com.crispinlab.composition.testsupport.CompositionAppControllerDescribeSpec
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.application.port.incoming.user.UserSearching
import com.crispinlab.user.application.port.incoming.user.UserSearching.Request
import com.crispinlab.user.application.port.incoming.user.UserSearching.Result
import com.crispinlab.user.domain.user.Handle
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
        val useCase = mockk<UserSearching>()
        val spaceMembershipLookup = mockk<SpaceMembershipLookup>()
        val controller = UserSearchingCompositionController(useCase, spaceMembershipLookup)

        beforeEach {
            clearMocks(useCase, spaceMembershipLookup)
            every { spaceMembershipLookup.membershipsOf(any()) } returns
                mapOf(
                    UserId(1L) to setOf(SpaceId(20L), SpaceId(10L)),
                    UserId(2L) to setOf(SpaceId(10L))
                )
        }

        describe("사용자 검색") {
            it("정상 검색 시 200 과 매칭 결과 + 소속 스페이스 집합을 반환한다") {
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
                        get("/v1/users")
                            .withAuth()
                            .param("query", "ali")
                            .param("size", "10")
                    ).then(
                        status().isOk,
                        jsonPath("$.items.length()").value(2),
                        jsonPath("$.items[0].userId").value("1"),
                        jsonPath("$.items[0].handle").value("alice"),
                        jsonPath("$.items[0].memberOfSpaceIds.length()").value(2),
                        jsonPath("$.items[0].memberOfSpaceIds[0]").value("10"),
                        jsonPath("$.items[0].memberOfSpaceIds[1]").value("20"),
                        jsonPath("$.items[1].userId").value("2"),
                        jsonPath("$.items[1].handle").value("alice_kim"),
                        jsonPath("$.items[1].memberOfSpaceIds.length()").value(1),
                        jsonPath("$.items[1].memberOfSpaceIds[0]").value("10")
                    ).document(
                        authHeader(required = true),
                        queryParameters(
                            "query" isParameterFor "검색어 (handle 부분 일치, 대소문자 무시, 1~30자)",
                            "size" isParameterFor
                                "결과 수 (1 ~ 20, 기본값 10)" isOptional true
                        ),
                        responseFields {
                            "items".array("매칭된 사용자 목록") {
                                "userId".string("사용자 식별자")
                                "handle".string("사용자 이름")
                                "memberOfSpaceIds".array(
                                    "사용자가 소속된 스페이스 식별자 목록 (SpaceId 오름차순, 소속 없으면 빈 배열)"
                                )
                            }
                        },
                        responseSchema = "UserSearchResponse"
                    )
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

            it("distinct userIds 에 대해 membershipsOf 를 정확히 1회 batch 호출한다") {
                every { useCase.perform(any()) } returns
                    Result(
                        items =
                            listOf(
                                Result.Item(userId = UserId(1L), handle = Handle("alice")),
                                Result.Item(userId = UserId(1L), handle = Handle("alice")),
                                Result.Item(userId = UserId(2L), handle = Handle("bob_kim"))
                            )
                    )

                controller
                    .`when`(
                        get("/v1/users")
                            .withAuth()
                            .param("query", "a")
                    ).then(status().isOk)

                verify(exactly = 1) {
                    spaceMembershipLookup.membershipsOf(setOf(UserId(1L), UserId(2L)))
                }
            }

            it("소속 스페이스가 없는 사용자는 memberOfSpaceIds 를 빈 배열로 응답한다") {
                every { spaceMembershipLookup.membershipsOf(any()) } returns emptyMap()
                every { useCase.perform(any()) } returns
                    Result(
                        items =
                            listOf(
                                Result.Item(userId = UserId(999L), handle = Handle("loner"))
                            )
                    )

                controller
                    .`when`(
                        get("/v1/users")
                            .withAuth()
                            .param("query", "loner")
                    ).then(
                        status().isOk,
                        jsonPath("$.items[0].userId").value("999"),
                        jsonPath("$.items[0].memberOfSpaceIds.length()").value(0)
                    )
            }

            it("membershipsOf 가 예외를 던져도 검색 결과 자체는 반환하고 memberOfSpaceIds 는 빈 배열로 응답한다") {
                every { spaceMembershipLookup.membershipsOf(any()) } throws
                    RuntimeException("lookup 실패")
                every { useCase.perform(any()) } returns
                    Result(
                        items =
                            listOf(
                                Result.Item(userId = UserId(1L), handle = Handle("alice"))
                            )
                    )

                controller
                    .`when`(
                        get("/v1/users")
                            .withAuth()
                            .param("query", "ali")
                    ).then(
                        status().isOk,
                        jsonPath("$.items.length()").value(1),
                        jsonPath("$.items[0].userId").value("1"),
                        jsonPath("$.items[0].handle").value("alice"),
                        jsonPath("$.items[0].memberOfSpaceIds.length()").value(0)
                    )
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
                verify(exactly = 0) { spaceMembershipLookup.membershipsOf(any()) }
            }

            it("query 가 빈 문자열이면 400 을 반환한다") {
                controller
                    .`when`(
                        get("/v1/users")
                            .withAuth()
                            .param("query", "")
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
                verify(exactly = 0) { spaceMembershipLookup.membershipsOf(any()) }
            }

            it("size 가 21 이상이면 400 을 반환한다") {
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
                verify(exactly = 0) { useCase.perform(any()) }
                verify(exactly = 0) { spaceMembershipLookup.membershipsOf(any()) }
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
                verify(exactly = 0) { spaceMembershipLookup.membershipsOf(any()) }
            }
        }
    })
