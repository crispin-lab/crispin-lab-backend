package com.crispinlab.composition.adapter.web.audit

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.composition.application.port.incoming.audit.SpaceAuditEntryListingComposition
import com.crispinlab.composition.application.port.incoming.audit.SpaceAuditEntryListingComposition.Result
import com.crispinlab.composition.testsupport.CompositionAppControllerDescribeSpec
import com.crispinlab.space.domain.audit.SpaceAuditAction
import com.crispinlab.space.domain.audit.SpaceAuditEntryId
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.user.domain.user.UserId
import com.crispinlab.user.testsupport.withAuth
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class SpaceAuditEntryListingCompositionControllerTest :
    CompositionAppControllerDescribeSpec(tag = "SpaceAudit", body = {
        val useCase = mockk<SpaceAuditEntryListingComposition>()
        val controller = SpaceAuditEntryListingCompositionController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("스페이스 감사 이력 조회") {
            it("정상 응답 시 200 과 최신순 페이지를 반환한다") {
                every { useCase.perform(any()) } returns
                    PageResult(
                        items =
                            listOf(
                                Result(
                                    id = SpaceAuditEntryId(2L),
                                    actorUserId = UserId(100L),
                                    actorHandle = "alice",
                                    action = SpaceAuditAction.EDITED,
                                    changeSummary = """{"name":{"before":"a","after":"b"}}""",
                                    createdAt = DUMMY_INSTANT
                                ),
                                Result(
                                    id = SpaceAuditEntryId(1L),
                                    actorUserId = UserId(100L),
                                    actorHandle = "alice",
                                    action = SpaceAuditAction.REGISTERED,
                                    changeSummary = """{"name":"a"}""",
                                    createdAt = DUMMY_INSTANT
                                )
                            ),
                        page = 0,
                        size = 20,
                        totalElements = 2L
                    )

                controller
                    .`when`(
                        get("/v1/spaces/{spaceId}/audit-entries", 10).withAuth()
                    ).then(
                        status().isOk,
                        jsonPath("$.items.length()").value(2),
                        jsonPath("$.items[0].id").value("2"),
                        jsonPath("$.items[0].action").value("EDITED"),
                        jsonPath("$.items[0].actorHandle").value("alice"),
                        jsonPath("$.items[1].id").value("1"),
                        jsonPath("$.items[1].action").value("REGISTERED"),
                        jsonPath("$.totalElements").value(2)
                    ).document(
                        authHeader(required = true),
                        pagingParameters(),
                        responseFields {
                            "items".array("감사 이력 목록") {
                                "id".string("감사 이력 식별자")
                                "actorUserId".string("변경 주체 사용자 식별자")
                                "actorHandle".string("변경 주체 handle (조회 miss 시 빈 문자열)")
                                "action".string("변경 유형 (REGISTERED / EDITED / DELETED)")
                                "changeSummary".string("변경 요약 JSON 문자열")
                                "createdAt".datetime("발생 시각")
                            }
                            "page".number("현재 페이지")
                            "size".number("페이지당 항목 수")
                            "totalElements".number("총 항목 수")
                            "totalPages".number("총 페이지 수")
                            "hasNext".boolean("다음 페이지 존재 여부")
                            "isEmpty".boolean("결과 비어 있음 여부")
                        },
                        responseSchema = "SpaceAuditEntryListResponse"
                    )
            }

            it("Authorization 헤더가 없으면 401 로 응답한다") {
                controller
                    .`when`(get("/v1/spaces/{spaceId}/audit-entries", 10))
                    .then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
