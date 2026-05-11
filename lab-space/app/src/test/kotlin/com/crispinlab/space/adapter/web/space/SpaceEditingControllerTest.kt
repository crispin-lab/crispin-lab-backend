package com.crispinlab.space.adapter.web.space

import com.crispinlab.space.adapter.web.GlobalExceptionHandler
import com.crispinlab.space.adapter.web.WebMvcConfig
import com.crispinlab.space.adapter.web.auth.AuthArgumentResolver
import com.crispinlab.space.application.port.incoming.space.SpaceEditing
import com.crispinlab.space.application.port.incoming.space.SpaceEditing.Result
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.resourceDetails
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.put

@WebMvcTest(SpaceEditingController::class)
@AutoConfigureRestDocs
@Import(WebMvcConfig::class, AuthArgumentResolver::class, GlobalExceptionHandler::class)
class SpaceEditingControllerTest : DescribeSpec() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var useCase: SpaceEditing

    init {
        extensions(SpringExtension())

        beforeEach {
            clearMocks(useCase)
        }

        describe("PUT /v1/spaces/{spaceId}") {
            it("이름·설명 변경에 성공하면 200 과 갱신 결과를 반환한다") {
                every { useCase.perform(any()) } returns
                    Result(
                        spaceId = "1",
                        name = "새 이름",
                        description = "새 설명",
                        updatedAt = DUMMY_INSTANT
                    )

                mockMvc
                    .put("/v1/spaces/{spaceId}", 1) {
                        header("X-User-Id", "100")
                        contentType = MediaType.APPLICATION_JSON
                        content = """{"name":"새 이름","description":"새 설명"}"""
                    }.andExpect {
                        status { isOk() }
                        jsonPath("$.spaceId") { value("1") }
                        jsonPath("$.name") { value("새 이름") }
                        jsonPath("$.description") { value("새 설명") }
                        jsonPath("$.updatedAt") { exists() }
                    }.andDo {
                        handle(
                            MockMvcRestDocumentationWrapper.document(
                                "space-edit",
                                resourceDetails()
                                    .tag("Space")
                                    .summary("스페이스 수정")
                                    .description("스페이스의 이름·설명을 부분 변경한다.")
                            )
                        )
                    }
            }

            it("X-User-Id 헤더가 없으면 400 을 반환한다") {
                mockMvc
                    .put("/v1/spaces/{spaceId}", 1) {
                        contentType = MediaType.APPLICATION_JSON
                        content = """{"name":"새 이름","description":"새 설명"}"""
                    }.andExpect {
                        status { isBadRequest() }
                    }
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("X-User-Id 가 숫자가 아니면 400 을 반환한다") {
                mockMvc
                    .put("/v1/spaces/{spaceId}", 1) {
                        header("X-User-Id", "not-a-number")
                        contentType = MediaType.APPLICATION_JSON
                        content = """{"name":"새 이름","description":"새 설명"}"""
                    }.andExpect {
                        status { isBadRequest() }
                    }
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    }
}
