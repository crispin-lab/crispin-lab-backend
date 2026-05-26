package com.crispinlab.space.adapter.web.spacemember

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberListing
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberListing.Summary
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.spacemember.SpaceMemberId
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.SpaceAppControllerDescribeSpec
import com.crispinlab.user.domain.user.UserId
import com.crispinlab.user.testsupport.withAuth
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class SpaceMemberListingControllerTest :
    SpaceAppControllerDescribeSpec(tag = "SpaceMember", body = {
        val useCase = mockk<SpaceMemberListing>()
        val controller = SpaceMemberListingController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("스페이스 멤버 목록 조회") {
            it("정상 응답 시 200 과 페이지를 반환한다") {
                every { useCase.perform(any()) } returns
                    PageResult(
                        items =
                            listOf(
                                Summary(
                                    spaceMemberId = SpaceMemberId(1L),
                                    spaceId = SpaceId(10L),
                                    userId = UserId(100L),
                                    role = SpaceMemberRole.OWNER,
                                    joinedAt = DUMMY_INSTANT
                                ),
                                Summary(
                                    spaceMemberId = SpaceMemberId(2L),
                                    spaceId = SpaceId(10L),
                                    userId = UserId(101L),
                                    role = SpaceMemberRole.MEMBER,
                                    joinedAt = DUMMY_INSTANT
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
                        jsonPath("$.items[0].role").value("OWNER"),
                        jsonPath("$.totalElements").value(2)
                    ).document(
                        authHeaderRequired(),
                        pagingParameters(),
                        responseFields {
                            "items".array("멤버 목록") {
                                "spaceMemberId".string("멤버 식별자")
                                "spaceId".string("스페이스 식별자")
                                "userId".string("사용자 식별자")
                                "role".string("역할")
                                "joinedAt".datetime("가입 시각")
                            }
                            "page".number("현재 페이지")
                            "size".number("페이지당 항목 수")
                            "totalElements".number("총 항목 수")
                            "totalPages".number("총 페이지 수")
                            "hasNext".boolean("다음 페이지 존재 여부")
                            "isEmpty".boolean("결과 비어 있음 여부")
                        }
                    )
            }

            it("비로그인 상태에서도 200 으로 응답하고 Anonymous 컨텍스트로 UseCase 가 호출된다") {
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
        }
    })
