package com.crispinlab.notification.adapter.web.notification

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.notification.application.port.incoming.notification.NotificationListing
import com.crispinlab.notification.application.port.incoming.notification.NotificationListing.Summary
import com.crispinlab.notification.domain.notification.NotificationId
import com.crispinlab.notification.domain.notification.NotificationType
import com.crispinlab.notification.domain.notification.SourceType
import com.crispinlab.notification.testsupport.NotificationAppControllerDescribeSpec
import com.crispinlab.user.domain.user.UserId
import com.crispinlab.user.testsupport.withAuth
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class NotificationListingControllerTest :
    NotificationAppControllerDescribeSpec(tag = "Notification", body = {
        val useCase = mockk<NotificationListing>()
        val controller = NotificationListingController(useCase)
        val occurredAt: Instant = Instant.parse("2026-01-01T00:00:00Z")

        beforeEach { clearMocks(useCase) }

        describe("알림 인박스 조회") {
            it("정상 응답 시 200 과 페이지를 반환한다") {
                every { useCase.perform(any()) } returns
                    PageResult(
                        items =
                            listOf(
                                Summary(
                                    notificationId = NotificationId(1L),
                                    type = NotificationType.MENTION,
                                    sourceType = SourceType.PAGE,
                                    sourceId = 10L,
                                    actorUserId = UserId(999L),
                                    isRead = false,
                                    createdAt = occurredAt,
                                    readAt = null
                                )
                            ),
                        page = 0,
                        size = 20,
                        totalElements = 1L
                    )

                controller
                    .`when`(
                        get("/v1/notifications")
                            .withAuth()
                            .param("page", "0")
                            .param("size", "20")
                    ).then(
                        status().isOk,
                        jsonPath("$.items.length()").value(1),
                        jsonPath("$.items[0].notificationId").value("1"),
                        jsonPath("$.items[0].type").value("MENTION"),
                        jsonPath("$.items[0].sourceType").value("PAGE"),
                        jsonPath("$.items[0].sourceId").value(10),
                        jsonPath("$.items[0].actorUserId").value("999"),
                        jsonPath("$.items[0].isRead").value(false),
                        jsonPath("$.totalElements").value(1)
                    ).document(
                        authHeader(required = true),
                        pagingParameters(),
                        responseFields {
                            "items".array("알림 목록") {
                                "notificationId".string("알림 식별자")
                                "type".string("알림 타입 (MENTION 등)")
                                "sourceType".string("source 타입 (PAGE / COMMENT)")
                                "sourceId".number("source 식별자")
                                "actorUserId".string("알림을 발생시킨 사용자 식별자")
                                "isRead".boolean("읽음 여부")
                                "createdAt".datetime("생성 시각")
                                "readAt".datetime("읽음 처리 시각 (미독이면 null)", optional = true)
                            }
                            "page".number("현재 페이지")
                            "size".number("페이지당 항목 수")
                            "totalElements".number("총 항목 수")
                            "totalPages".number("총 페이지 수")
                            "hasNext".boolean("다음 페이지 존재 여부")
                            "isEmpty".boolean("결과 비어 있음 여부")
                        },
                        responseSchema = "NotificationListResponse"
                    )
            }

            it("unreadOnly=true 파라미터가 UseCase 에 전달된다") {
                every { useCase.perform(match { it.unreadOnly }) } returns
                    PageResult(
                        items = emptyList(),
                        page = 0,
                        size = 20,
                        totalElements = 0L
                    )

                controller
                    .`when`(
                        get("/v1/notifications")
                            .withAuth()
                            .param("unreadOnly", "true")
                    ).then(status().isOk)

                verify(exactly = 1) { useCase.perform(match { it.unreadOnly }) }
            }

            it("Authorization 토큰이 없으면 401 을 반환한다") {
                controller
                    .`when`(get("/v1/notifications"))
                    .then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
