package com.crispinlab.space.adapter.web.space

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec.FieldBuilder.Companion.responseFields
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.incoming.space.SpaceListing
import com.crispinlab.space.application.port.incoming.space.SpaceListing.Summary
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.SpaceAppControllerDescribeSpec
import com.crispinlab.user.domain.user.AuthContext
import com.crispinlab.user.testsupport.withAuth
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class SpaceListingControllerTest :
    SpaceAppControllerDescribeSpec(tag = "Space", body = {
        val useCase = mockk<SpaceListing>()
        val controller = SpaceListingController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("스페이스 목록 조회") {
            it("정상 응답 시 200 과 페이지를 반환한다") {
                every { useCase.perform(any()) } returns
                    PageResult(
                        items =
                            listOf(
                                Summary(
                                    spaceId = SpaceId(2L),
                                    name = "최근",
                                    description = "최근 설명",
                                    visibility = SpaceVisibility.INTERNAL,
                                    createdAt = DUMMY_INSTANT,
                                    updatedAt = DUMMY_INSTANT
                                ),
                                Summary(
                                    spaceId = SpaceId(1L),
                                    name = "이전",
                                    description = "이전 설명",
                                    visibility = SpaceVisibility.PUBLIC,
                                    createdAt = DUMMY_INSTANT,
                                    updatedAt = DUMMY_INSTANT
                                )
                            ),
                        page = 0,
                        size = 20,
                        totalElements = 2L
                    )

                controller
                    .`when`(
                        get("/v1/spaces")
                            .withAuth()
                            .param("page", "0")
                            .param("size", "20")
                    ).then(
                        status().isOk,
                        jsonPath("$.items.length()").value(2),
                        jsonPath("$.items[0].spaceId").value("2"),
                        jsonPath("$.items[0].name").value("최근"),
                        jsonPath("$.page").value(0),
                        jsonPath("$.size").value(20),
                        jsonPath("$.totalElements").value(2),
                        jsonPath("$.totalPages").value(1),
                        jsonPath("$.hasNext").value(false)
                    ).document(
                        authHeaderRequired(),
                        pagingParameters(),
                        responseFields {
                            "items".array("스페이스 목록") {
                                "spaceId".string("스페이스 식별자")
                                "name".string("이름")
                                "description".string("설명")
                                "visibility".string("공개 범위")
                                "createdAt".datetime("생성 시각")
                                "updatedAt".datetime("최근 갱신 시각")
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

            it("page/size 파라미터가 없어도 기본값으로 200 을 반환한다") {
                every { useCase.perform(any()) } returns
                    PageResult(
                        items = emptyList(),
                        page = 0,
                        size = 20,
                        totalElements = 0L
                    )

                controller
                    .`when`(get("/v1/spaces").withAuth())
                    .then(
                        status().isOk,
                        jsonPath("$.items.length()").value(0),
                        jsonPath("$.totalElements").value(0)
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
                    .`when`(get("/v1/spaces"))
                    .then(
                        status().isOk,
                        jsonPath("$.items.length()").value(0)
                    )
                verify {
                    useCase.perform(
                        match { it.auth == AuthContext.Anonymous }
                    )
                }
            }

            it("page 가 음수면 400 과 한국어 메시지를 반환한다") {
                controller
                    .`when`(
                        get("/v1/spaces")
                            .withAuth()
                            .param("page", "-1")
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST"),
                        jsonPath("$.message").value("페이지 번호는 0 이상이어야 합니다.")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("page 가 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(
                        get("/v1/spaces")
                            .withAuth()
                            .param("page", "abc")
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("size 가 허용 범위를 벗어나면 400 과 한국어 메시지를 반환한다") {
                listOf("-1", "0", "201").forEach { invalidSize ->
                    controller
                        .`when`(
                            get("/v1/spaces")
                                .withAuth()
                                .param("size", invalidSize)
                        ).then(
                            status().isBadRequest,
                            jsonPath("$.code").value("INVALID_REQUEST"),
                            jsonPath("$.message").value("페이지 크기는 1 이상 200 이하여야 합니다.")
                        )
                }
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("size 가 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(
                        get("/v1/spaces")
                            .withAuth()
                            .param("size", "abc")
                    ).then(
                        status().isBadRequest,
                        jsonPath("$.code").value("INVALID_REQUEST")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
