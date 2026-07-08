package com.crispinlab.composition.adapter.web.space

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.composition.application.port.incoming.space.SpaceListingComposition
import com.crispinlab.composition.application.port.incoming.space.SpaceListingComposition.LatestPage
import com.crispinlab.composition.application.port.incoming.space.SpaceListingComposition.Result
import com.crispinlab.composition.testsupport.CompositionAppControllerDescribeSpec
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.user.testsupport.withAuth
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hamcrest.Matchers.nullValue
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class SpaceListingCompositionControllerTest :
    CompositionAppControllerDescribeSpec(tag = "Space", body = {
        val useCase = mockk<SpaceListingComposition>()
        val controller = SpaceListingCompositionController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("스페이스 목록 조회") {
            it("정상 응답 시 200 과 조립된 목록을 반환한다") {
                every { useCase.perform(any()) } returns
                    PageResult(
                        items =
                            listOf(
                                Result(
                                    spaceId = SpaceId(10L),
                                    name = "팀 위키",
                                    description = "팀 공용 위키",
                                    visibility = SpaceVisibility.INTERNAL,
                                    myRole = SpaceMemberRole.OWNER,
                                    memberCount = 5L,
                                    pageCount = 12L,
                                    lastActivityAt = DUMMY_INSTANT.plusSeconds(3600),
                                    latestPage =
                                        LatestPage(
                                            pageId = PageId(555L),
                                            title = "오늘의 회고",
                                            updatedAt = DUMMY_INSTANT.plusSeconds(3600)
                                        ),
                                    createdAt = DUMMY_INSTANT,
                                    updatedAt = DUMMY_INSTANT
                                ),
                                Result(
                                    spaceId = SpaceId(20L),
                                    name = "공지사항",
                                    description = "전체 공지",
                                    visibility = SpaceVisibility.PUBLIC,
                                    myRole = null,
                                    memberCount = 3L,
                                    pageCount = 0L,
                                    lastActivityAt = DUMMY_INSTANT,
                                    latestPage = null,
                                    createdAt = DUMMY_INSTANT,
                                    updatedAt = DUMMY_INSTANT
                                )
                            ),
                        page = 0,
                        size = 20,
                        totalElements = 2L
                    )

                controller
                    .`when`(get("/v1/spaces").withAuth())
                    .then(
                        status().isOk,
                        jsonPath("$.items.length()").value(2),
                        jsonPath("$.items[0].spaceId").value("10"),
                        jsonPath("$.items[0].myRole").value("OWNER"),
                        jsonPath("$.items[0].memberCount").value(5),
                        jsonPath("$.items[0].pageCount").value(12),
                        jsonPath("$.items[0].latestPage.pageId").value("555"),
                        jsonPath("$.items[0].latestPage.title").value("오늘의 회고"),
                        jsonPath("$.items[1].myRole").value(nullValue()),
                        jsonPath("$.items[1].latestPage").value(nullValue()),
                        jsonPath("$.items[1].pageCount").value(0),
                        jsonPath("$.totalElements").value(2)
                    ).document(
                        authHeader(required = false),
                        pagingParameters(),
                        responseFields {
                            "items".array("스페이스 목록") {
                                "spaceId".string("스페이스 식별자")
                                "name".string("스페이스 이름")
                                "description".string("스페이스 설명")
                                "visibility".string(
                                    description = "스페이스 공개 범위",
                                    enum = SpaceVisibility.entries.map { it.name }
                                )
                                "myRole".string(
                                    description =
                                        "현재 viewer 의 role (Anonymous / 비-멤버는 null)",
                                    optional = true,
                                    enum = SpaceMemberRole.entries.map { it.name }
                                )
                                "memberCount".number("스페이스 멤버 수 (lookup 실패 시 0)")
                                "pageCount".number(
                                    "viewer 가 볼 수 있는 페이지 수 (lookup 실패 시 0)"
                                )
                                "lastActivityAt".datetime(
                                    "MAX(space.updatedAt, latestPage.updatedAt), 페이지 없으면 space.updatedAt"
                                )
                                "latestPage".`object`(
                                    description =
                                        "viewer 가 볼 수 있는 최근 편집 페이지 (없거나 lookup 실패 시 null)",
                                    optional = true
                                ) {
                                    "pageId".string("페이지 식별자")
                                    "title".string("페이지 제목")
                                    "updatedAt".datetime("페이지 편집 시각")
                                }
                                "createdAt".datetime("스페이스 생성 시각")
                                "updatedAt".datetime("스페이스 갱신 시각")
                            }
                            "page".number("현재 페이지")
                            "size".number("페이지당 항목 수")
                            "totalElements".number("총 항목 수")
                            "totalPages".number("총 페이지 수")
                            "hasNext".boolean("다음 페이지 존재 여부")
                            "isEmpty".boolean("결과 비어 있음 여부")
                        },
                        responseSchema = "SpaceListResponse"
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
                    .`when`(get("/v1/spaces"))
                    .then(status().isOk)
                verify {
                    useCase.perform(match { it.viewer == Viewer.Anonymous })
                }
            }

            it("옵셔널 endpoint 라도 Authorization 헤더가 잘못되면 401 로 fail-fast 한다") {
                controller
                    .`when`(
                        get("/v1/spaces")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-valid-token")
                    ).then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("page 가 음수면 Request 생성 시점의 IAE 가 400 으로 매핑되고 UseCase 는 호출되지 않는다") {
                controller
                    .`when`(
                        get("/v1/spaces")
                            .withAuth()
                            .param("page", "-1")
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
