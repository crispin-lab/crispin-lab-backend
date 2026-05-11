package com.crispinlab.space.adapter.web.space

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.space.adapter.web.auth.AuthArgumentResolver
import com.crispinlab.space.application.port.incoming.space.SpaceGetting
import com.crispinlab.space.application.port.incoming.space.SpaceGetting.Result
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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(SpaceGettingController::class)
@AutoConfigureRestDocs
@Import(WebMvcConfig::class, AuthArgumentResolver::class, GlobalExceptionHandler::class)
class SpaceGettingControllerTest : DescribeSpec() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var useCase: SpaceGetting

    init {
        extensions(SpringExtension())

        describe("GET /v1/spaces/{spaceId}") {
            it("스페이스가 존재하면 200 과 정보를 반환한다") {
                every { useCase.perform(any()) } returns
                    Result(
                        spaceId = 1L,
                        name = "팀 위키",
                        description = "공유 공간",
                        createdAt = DUMMY_INSTANT,
                        updatedAt = DUMMY_INSTANT
                    )

                mockMvc
                    .get("/v1/spaces/1") {
                        header("X-User-Id", "100")
                    }.andExpect {
                        status { isOk() }
                        jsonPath("$.spaceId") { value(1) }
                        jsonPath("$.name") { value("팀 위키") }
                    }
            }

            it("스페이스가 없으면 404 를 반환한다") {
                every { useCase.perform(any()) } throws NotFoundException("스페이스를 찾을 수 없습니다.")

                mockMvc
                    .get("/v1/spaces/999") {
                        header("X-User-Id", "100")
                    }.andExpect {
                        status { isNotFound() }
                        jsonPath("$.message") { value("스페이스를 찾을 수 없습니다.") }
                    }
            }
        }
    }
}
