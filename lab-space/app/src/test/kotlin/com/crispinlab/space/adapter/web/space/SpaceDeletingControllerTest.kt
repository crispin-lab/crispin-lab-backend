package com.crispinlab.space.adapter.web.space

import com.crispinlab.space.adapter.web.GlobalExceptionHandler
import com.crispinlab.space.adapter.web.WebMvcConfig
import com.crispinlab.space.adapter.web.auth.AuthArgumentResolver
import com.crispinlab.space.application.port.incoming.space.SpaceDeleting
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete

@WebMvcTest(SpaceDeletingController::class)
@AutoConfigureRestDocs
@Import(WebMvcConfig::class, AuthArgumentResolver::class, GlobalExceptionHandler::class)
class SpaceDeletingControllerTest : DescribeSpec() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var useCase: SpaceDeleting

    init {
        extensions(SpringExtension())

        beforeEach {
            clearMocks(useCase)
        }

        describe("DELETE /v1/spaces/{spaceId}") {
            it("삭제에 성공하면 204 를 반환한다") {
                every { useCase.perform(any()) } just runs

                mockMvc
                    .delete("/v1/spaces/1") {
                        header("X-User-Id", "100")
                    }.andExpect {
                        status { isNoContent() }
                    }.andDo {
                        handle(MockMvcRestDocumentationWrapper.document("space-delete"))
                    }
            }

            it("X-User-Id 헤더가 없으면 400 을 반환한다") {
                mockMvc
                    .delete("/v1/spaces/1")
                    .andExpect {
                        status { isBadRequest() }
                    }
            }

            it("X-User-Id 가 숫자가 아니면 400 을 반환한다") {
                mockMvc
                    .delete("/v1/spaces/1") {
                        header("X-User-Id", "not-a-number")
                    }.andExpect {
                        status { isBadRequest() }
                    }
            }
        }
    }
}
