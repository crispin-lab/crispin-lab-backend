package com.crispinlab.notification.adapter.web.notification

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.notification.application.port.incoming.notification.NotificationReading
import com.crispinlab.notification.domain.notification.NotificationErrorCode
import com.crispinlab.notification.testsupport.NotificationAppControllerDescribeSpec
import com.crispinlab.user.testsupport.withAuth
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class NotificationReadingControllerTest :
    NotificationAppControllerDescribeSpec(tag = "Notification", body = {
        val useCase = mockk<NotificationReading>()
        val controller = NotificationReadingController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("알림 읽음 처리") {
            it("정상 처리 시 200 을 반환한다") {
                every {
                    useCase.perform(
                        match {
                            it.notificationId.value == 42L &&
                                it.viewer.userId.value == 100L
                        }
                    )
                } returns Unit

                controller
                    .`when`(
                        post("/v1/notifications/{notificationId}/read", 42).withAuth()
                    ).then(status().isOk)
                    .document(authHeader(required = true))

                verify(exactly = 1) {
                    useCase.perform(
                        match {
                            it.notificationId.value == 42L &&
                                it.viewer.userId.value == 100L
                        }
                    )
                }
            }

            it("알림이 없으면 404 를 반환한다") {
                every { useCase.perform(any()) } throws
                    NotFoundException(NotificationErrorCode.NOTIFICATION_NOT_FOUND)

                controller
                    .`when`(
                        post("/v1/notifications/{notificationId}/read", 999).withAuth()
                    ).then(
                        status().isNotFound,
                        jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"),
                        jsonPath("$.message").value("알림을 찾을 수 없습니다.")
                    )
            }

            it("Authorization 토큰이 없으면 401 을 반환한다") {
                controller
                    .`when`(post("/v1/notifications/{notificationId}/read", 1))
                    .then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("notificationId 형식이 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(
                        post("/v1/notifications/{notificationId}/read", "not-a-number")
                            .withAuth()
                    ).then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
