package com.crispinlab.user.adapter.web.session

import com.crispinlab.user.application.port.incoming.auth.AuthLogout
import com.crispinlab.user.application.port.incoming.auth.AuthLogout.Request
import com.crispinlab.user.testsupport.Fixtures.basicSessionToken
import com.crispinlab.user.testsupport.UserAppControllerDescribeSpec
import com.crispinlab.user.testsupport.withAuth
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class SessionDeletionControllerTest :
    UserAppControllerDescribeSpec(tag = "Session", body = {
        val useCase = mockk<AuthLogout>()
        val controller = SessionDeletionController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("현재 세션 로그아웃") {
            it("정상 인증 시 204 를 반환한다") {
                every { useCase.perform(any()) } just runs

                controller
                    .`when`(
                        delete("/v1/sessions/me").withAuth()
                    ).then(status().isNoContent)
                    .document(authHeaderRequired())

                verify(exactly = 1) { useCase.perform(any()) }
            }

            it("Auth 의 sessionToken 을 Request 로 전달한다") {
                val token = basicSessionToken()
                val captured = slot<Request>()
                every { useCase.perform(capture(captured)) } just runs

                controller
                    .`when`(
                        delete("/v1/sessions/me").withAuth(sessionToken = token)
                    ).then(status().isNoContent)

                captured.captured.token shouldBe token
            }

            it("Authorization 헤더가 없으면 401 을 반환하고 use case 를 호출하지 않는다") {
                controller
                    .`when`(delete("/v1/sessions/me"))
                    .then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
