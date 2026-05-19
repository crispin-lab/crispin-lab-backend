package com.crispinlab.user.adapter.web.auth

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.requestFields
import com.crispinlab.user.application.port.incoming.auth.AuthLogout
import com.crispinlab.user.testsupport.Fixtures.basicSessionToken
import com.crispinlab.user.testsupport.UserAppControllerDescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AuthLogoutControllerTest :
    UserAppControllerDescribeSpec(tag = "Auth", body = {
        val useCase = mockk<AuthLogout>()
        val controller = AuthLogoutController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("로그아웃") {
            it("정상 토큰을 받으면 204 를 반환한다") {
                val token = basicSessionToken()
                every { useCase.perform(any()) } returns Unit

                controller
                    .`when`(
                        post("/v1/auth/logout")
                            .body(mapOf("token" to token.value))
                    ).then(
                        status().isNoContent
                    ).document(
                        requestFields {
                            "token".string("revoke 대상 세션 토큰")
                        }
                    )
                verify(exactly = 1) { useCase.perform(any()) }
            }

            it("미존재 토큰이어도 204 (멱등)") {
                val unknown = basicSessionToken(body = "z".repeat(43))
                every { useCase.perform(any()) } returns Unit

                controller
                    .`when`(
                        post("/v1/auth/logout")
                            .body(mapOf("token" to unknown.value))
                    ).then(status().isNoContent)
                verify(exactly = 1) { useCase.perform(any()) }
            }

            it("형식이 깨진 토큰도 204 (멱등)") {
                every { useCase.perform(any()) } returns Unit

                controller
                    .`when`(
                        post("/v1/auth/logout")
                            .body(mapOf("token" to "not-a-session-token"))
                    ).then(status().isNoContent)
                verify(exactly = 1) { useCase.perform(any()) }
            }
        }
    })
