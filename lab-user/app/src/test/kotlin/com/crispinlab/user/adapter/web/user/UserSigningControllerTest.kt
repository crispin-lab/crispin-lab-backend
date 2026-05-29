package com.crispinlab.user.adapter.web.user

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.requestFields
import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.common.exception.ConflictException
import com.crispinlab.user.application.port.incoming.user.UserSigning
import com.crispinlab.user.application.port.incoming.user.UserSigning.Result
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

class UserSigningControllerTest :
    UserAppControllerDescribeSpec(tag = "User", body = {
        val useCase = mockk<UserSigning>()
        val controller = UserSigningController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("회원가입") {
            it("정상 가입 시 201 과 userId/token 을 반환한다") {
                val token = basicSessionToken()
                every {
                    useCase.perform(
                        match {
                            it.email.value == "user@example.com" &&
                                it.handle.value == "test_user" &&
                                it.password == "pass1234"
                        }
                    )
                } returns Result(userId = UserId(42L), token = token)

                controller
                    .`when`(
                        post("/v1/users")
                            .body(
                                mapOf(
                                    "email" to "user@example.com",
                                    "handle" to "test_user",
                                    "password" to "pass1234"
                                )
                            )
                    ).then(
                        status().isCreated,
                        jsonPath("$.userId").value("42"),
                        jsonPath("$.token").value(token.value)
                    ).document(
                        requestFields {
                            "email".string("이메일")
                            "handle".string("핸들 (영문 소문자/숫자/_, 3~30자)")
                            "password".string("비밀번호")
                        },
                        responseFields {
                            "userId".string("생성된 사용자 식별자")
                            "token".string("발급된 세션 토큰")
                        },
                        requestSchema = "UserSignupRequest",
                        responseSchema = "UserSignupResponse"
                    )
            }

            it("이메일이 중복되면 409 를 반환한다") {
                every { useCase.perform(any()) } throws
                    ConflictException(UserErrorCode.EMAIL_DUPLICATED)

                controller
                    .`when`(
                        post("/v1/users")
                            .body(
                                mapOf(
                                    "email" to "user@example.com",
                                    "handle" to "test_user",
                                    "password" to "pass1234"
                                )
                            )
                    ).then(
                        status().isConflict,
                        jsonPath("$.code").value("EMAIL_DUPLICATED")
                    )
                verify(exactly = 1) { useCase.perform(any()) }
            }
        }
    })
