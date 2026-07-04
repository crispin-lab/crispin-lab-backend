package com.crispinlab.composition.adapter.web.spacemember

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.composition.application.port.incoming.spacemember.SpaceMemberListingComposition
import com.crispinlab.composition.application.port.incoming.spacemember.SpaceMemberListingComposition.Result
import com.crispinlab.composition.testsupport.CompositionAppControllerDescribeSpec
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.spacemember.SpaceMemberId
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.user.domain.user.UserId
import com.crispinlab.user.testsupport.withAuth
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class SpaceMemberListingCompositionControllerTest :
    CompositionAppControllerDescribeSpec(tag = "SpaceMember", body = {
        val useCase = mockk<SpaceMemberListingComposition>()
        val controller = SpaceMemberListingCompositionController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("스페이스 멤버 목록 조회") {
            it("정상 응답 시 200 과 페이지를 반환한다") {
                every { useCase.perform(any()) } returns
                    PageResult(
                        items =
                            listOf(
                                Result(
                                    spaceMemberId = SpaceMemberId(1L),
                                    spaceId = SpaceId(10L),
                                    userId = UserId(100L),
                                    role = SpaceMemberRole.OWNER,
                                    joinedAt = DUMMY_INSTANT,
                                    handle = "alice"
                                ),
                                Result(
                                    spaceMemberId = SpaceMemberId(2L),
                                    spaceId = SpaceId(10L),
                                    userId = UserId(101L),
                                    role = SpaceMemberRole.MEMBER,
                                    joinedAt = DUMMY_INSTANT,
                                    handle = "bob"
                                )
                            ),
                        page = 0,
                        size = 20,
                        totalElements = 2L
                    )

                controller
                    .`when`(
                        get("/v1/spaces/{spaceId}/members", 10).withAuth()
                    ).then(
                        status().isOk,
                        jsonPath("$.items.length()").value(2),
                        jsonPath("$.items[0].spaceMemberId").value("1"),
                        jsonPath("$.items[0].role").value("OWNER"),
                        jsonPath("$.items[0].handle").value("alice"),
                        jsonPath("$.items[1].spaceMemberId").value("2"),
                        jsonPath("$.items[1].handle").value("bob"),
                        jsonPath("$.totalElements").value(2)
                    ).document(
                        authHeader(required = false),
                        pagingParameters(),
                        responseFields {
                            "items".array("멤버 목록") {
                                "spaceMemberId".string("멤버 식별자")
                                "spaceId".string("스페이스 식별자")
                                "userId".string("사용자 식별자")
                                "role".string("역할")
                                "joinedAt".datetime("가입 시각")
                                "handle".string("사용자 handle (사용자 조회 miss 시 빈 문자열)")
                            }
                            "page".number("현재 페이지")
                            "size".number("페이지당 항목 수")
                            "totalElements".number("총 항목 수")
                            "totalPages".number("총 페이지 수")
                            "hasNext".boolean("다음 페이지 존재 여부")
                            "isEmpty".boolean("결과 비어 있음 여부")
                        },
                        responseSchema = "SpaceMemberListResponse"
                    )
            }

            it("비로그인 상태에서도 200 으로 응답하고 Anonymous viewer 로 UseCase 가 호출된다") {
                every { useCase.perform(any()) } returns
                    PageResult(
                        items = emptyList(),
                        page = 0,
                        size = 20,
                        totalElements = 0L
                    )

                controller
                    .`when`(get("/v1/spaces/{spaceId}/members", 10))
                    .then(status().isOk)
                verify {
                    useCase.perform(match { it.viewer == Viewer.Anonymous })
                }
            }

            it("옵셔널 endpoint 라도 Authorization 헤더가 잘못되면 401 로 fail-fast 한다") {
                controller
                    .`when`(
                        get("/v1/spaces/{spaceId}/members", 10)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-valid-token")
                    ).then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
