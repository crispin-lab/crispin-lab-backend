package com.crispinlab.user.adapter.web.user

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.user.application.port.incoming.user.UserMeRetrieving
import com.crispinlab.user.application.port.incoming.user.UserMeRetrieving.Request
import com.crispinlab.user.application.port.incoming.user.UserMeRetrieving.Result
import com.crispinlab.user.domain.user.EmailAddress
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.SystemRole
import com.crispinlab.user.domain.user.UserId
import com.crispinlab.user.testsupport.UserAppControllerDescribeSpec
import com.crispinlab.user.testsupport.withAuth
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class UserMeRetrievingControllerTest :
    UserAppControllerDescribeSpec(tag = "User", body = {
        val useCase = mockk<UserMeRetrieving>()
        val controller = UserMeRetrievingController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("현재 세션 사용자 조회") {
            it("정상 인증 시 200 과 자기 정보를 반환한다") {
                every { useCase.perform(any()) } returns
                    Result(
                        userId = UserId(42L),
                        handle = Handle("alice_kim"),
                        email = EmailAddress("alice@example.com"),
                        isAdmin = false
                    )

                controller
                    .`when`(
                        get("/v1/users/me").withAuth(userId = "42")
                    ).then(
                        status().isOk,
                        jsonPath("$.userId").value("42"),
                        jsonPath("$.handle").value("alice_kim"),
                        jsonPath("$.email").value("alice@example.com"),
                        jsonPath("$.isAdmin").value(false)
                    ).document(
                        authHeaderRequired(),
                        responseFields {
                            "userId".string("사용자 식별자")
                            "handle".string("핸들")
                            "email".string("이메일")
                            "isAdmin".boolean("ADMIN 권한 보유 여부")
                        },
                        responseSchema = "UserMeResponse"
                    )

                verify(exactly = 1) { useCase.perform(any()) }
            }

            it("ADMIN 사용자면 isAdmin 이 true 로 응답된다") {
                every { useCase.perform(any()) } returns
                    Result(
                        userId = UserId(7L),
                        handle = Handle("root_admin"),
                        email = EmailAddress("admin@example.com"),
                        isAdmin = true
                    )

                controller
                    .`when`(
                        get("/v1/users/me").withAuth(userId = "7", role = SystemRole.ADMIN)
                    ).then(
                        status().isOk,
                        jsonPath("$.isAdmin").value(true)
                    )
            }

            it("Auth 의 userId 가 Request 의 currentUserId 로 전달된다") {
                val captured = slot<Request>()
                every { useCase.perform(capture(captured)) } returns
                    Result(
                        userId = UserId(99L),
                        handle = Handle("bob_lee"),
                        email = EmailAddress("bob@example.com"),
                        isAdmin = false
                    )

                controller
                    .`when`(
                        get("/v1/users/me").withAuth(userId = "99")
                    ).then(status().isOk)

                captured.captured.currentUserId shouldBe UserId(99L)
            }

            it("Authorization 헤더가 없으면 401 을 반환하고 use case 를 호출하지 않는다") {
                controller
                    .`when`(get("/v1/users/me"))
                    .then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
