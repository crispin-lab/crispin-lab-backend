package com.crispinlab.space.adapter.web.space

import com.crispinlab.space.adapter.web.GlobalExceptionHandler
import com.crispinlab.space.adapter.web.WebMvcConfig
import com.crispinlab.space.adapter.web.auth.AuthArgumentResolver
import com.crispinlab.space.application.port.incoming.space.SpaceRegistering
import com.crispinlab.space.application.port.incoming.space.SpaceRegistering.Result
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
import org.springframework.test.web.servlet.post

@WebMvcTest(SpaceRegisteringController::class)
@AutoConfigureRestDocs
@Import(WebMvcConfig::class, AuthArgumentResolver::class, GlobalExceptionHandler::class)
class SpaceRegisteringControllerTest : DescribeSpec() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var useCase: SpaceRegistering

    init {
        extensions(SpringExtension())

        beforeEach {
            clearMocks(useCase)
        }

        describe("POST /v1/spaces") {
            it("스페이스 생성에 성공하면 201 과 spaceId 를 반환한다") {
                every {
                    useCase.perform(
                        match {
                            it.name == "팀 위키" &&
                                it.description == "공유 공간" &&
                                it.currentUserId.value == 100L
                        }
                    )
                } returns Result(spaceId = "42")

                mockMvc
                    .post("/v1/spaces") {
                        header("X-User-Id", "100")
                        contentType = MediaType.APPLICATION_JSON
                        content = """{"name":"팀 위키","description":"공유 공간"}"""
                    }.andExpect {
                        status { isCreated() }
                        jsonPath("$.spaceId") { value("42") }
                    }.andDo {
                        handle(
                            MockMvcRestDocumentationWrapper.document(
                                "space-register",
                                resourceDetails()
                                    .tag("Space")
                                    .summary("스페이스 생성")
                                    .description("새 스페이스를 생성하고 식별자를 반환한다.")
                            )
                        )
                    }
            }

            it("X-User-Id 헤더가 없으면 400 을 반환한다") {
                mockMvc
                    .post("/v1/spaces") {
                        contentType = MediaType.APPLICATION_JSON
                        content = """{"name":"팀 위키","description":"공유 공간"}"""
                    }.andExpect {
                        status { isBadRequest() }
                    }
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("X-User-Id 가 숫자가 아니면 400 을 반환한다") {
                mockMvc
                    .post("/v1/spaces") {
                        header("X-User-Id", "not-a-number")
                        contentType = MediaType.APPLICATION_JSON
                        content = """{"name":"팀 위키","description":"공유 공간"}"""
                    }.andExpect {
                        status { isBadRequest() }
                    }
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    }
}
