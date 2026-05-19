package com.crispinlab.user.adapter.web.auth

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.requestFields
import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.common.exception.AuthenticationException
import com.crispinlab.user.application.port.incoming.auth.AuthLogin
import com.crispinlab.user.application.port.incoming.auth.AuthLogin.Result
import com.crispinlab.user.domain.user.UserErrorCode
import com.crispinlab.user.domain.user.UserId
import com.crispinlab.user.testsupport.Fixtures.basicSessionToken
import com.crispinlab.user.testsupport.UserAppControllerDescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AuthLoginControllerTest :
    UserAppControllerDescribeSpec(tag = "Auth", body = {
        val useCase = mockk<AuthLogin>()
        val controller = AuthLoginController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("로그인") {
            it("정상 로그인 시 200 과 userId/token 을 반환한다") {
                val token = basicSessionToken()
                every {
                    useCase.perform(
                        match {
                            it.email == "user@example.com" && it.password == "pass1234"
                        }
                    )
                } returns Result(userId = UserId(42L), token = token)

                controller
                    .`when`(
                        post("/v1/auth/login")
                            .body(
                                mapOf(
                                    "email" to "user@example.com",
                                    "password" to "pass1234"
                                )
                            )
                    ).then(
                        status().isOk,
                        jsonPath("$.userId").value("42"),
                        jsonPath("$.token").value(token.value)
                    ).document(
                        requestFields {
                            "email".string("이메일")
                            "password".string("비밀번호")
                        },
                        responseFields {
                            "userId".string("사용자 식별자")
                            "token".string("발급된 세션 토큰")
                        }
                    )
            }

            it("자격증명이 잘못되면 401 + INVALID_CREDENTIALS 를 반환한다") {
                every { useCase.perform(any()) } throws
                    AuthenticationException(UserErrorCode.INVALID_CREDENTIALS)

                controller
                    .`when`(
                        post("/v1/auth/login")
                            .body(
                                mapOf(
                                    "email" to "user@example.com",
                                    "password" to "wrong"
                                )
                            )
                    ).then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_CREDENTIALS")
                    )
                verify(exactly = 1) { useCase.perform(any()) }
            }
        }
    })
