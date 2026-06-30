package com.crispinlab.user.adapter.web.user

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.user.application.port.incoming.user.UserSearching
import com.crispinlab.user.application.port.incoming.user.UserSearching.Request
import com.crispinlab.user.application.port.incoming.user.UserSearching.Result
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.UserId
import com.crispinlab.user.testsupport.UserAppControllerDescribeSpec
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

class UserSearchingControllerTest :
    UserAppControllerDescribeSpec(tag = "User", body = {
        val useCase = mockk<UserSearching>()
        val controller = UserSearchingController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("사용자 검색") {
            it("정상 검색 시 200 과 매칭 결과를 반환한다") {
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
                        jsonPath("$.items[1].userId").value("2"),
                        jsonPath("$.items[1].handle").value("alice_kim")
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

            it("query 가 누락되면 400 을 반환하고 use case 를 호출하지 않는다") {
                controller
                    .`when`(get("/v1/users").withAuth())
                    .then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
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
