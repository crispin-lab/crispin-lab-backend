package com.crispinlab.space.adapter.web.space

import com.crispinlab.space.adapter.web.auth.AuthArgumentResolver
import com.crispinlab.space.application.port.incoming.space.SpaceEditing
import com.crispinlab.space.application.port.incoming.space.SpaceEditing.Result
import com.crispinlab.space.config.GlobalExceptionHandler
import com.crispinlab.space.config.WebMvcConfig
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.every
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
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

        describe("PUT /v1/spaces/{spaceId}") {
            it("이름·설명 변경에 성공하면 200 과 갱신 결과를 반환한다") {
                every { useCase.perform(any()) } returns
                    Result(
                        spaceId = 1L,
                        name = "새 이름",
                        description = "새 설명",
                        updatedAt = DUMMY_INSTANT
                    )

                mockMvc
                    .put("/v1/spaces/1") {
                        header("X-User-Id", "100")
                        contentType = MediaType.APPLICATION_JSON
                        content = """{"name":"새 이름","description":"새 설명"}"""
                    }.andExpect {
                        status { isOk() }
                        jsonPath("$.name") { value("새 이름") }
                        jsonPath("$.description") { value("새 설명") }
                    }
            }
        }
    }
}
