package com.crispinlab.space.adapter.web.tag

import com.crispinlab.common.exception.ForbiddenException
import com.crispinlab.space.application.port.incoming.tag.TagDeleting
import com.crispinlab.space.domain.tag.TagErrorCode
import com.crispinlab.space.testsupport.SpaceAppControllerDescribeSpec
import com.crispinlab.user.domain.user.SystemRole
import com.crispinlab.user.testsupport.withAuth
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class TagDeletingControllerTest :
    SpaceAppControllerDescribeSpec(tag = "Tag", body = {
        val useCase = mockk<TagDeleting>()
        val controller = TagDeletingController(useCase)

        beforeEach { clearMocks(useCase) }

        describe("태그 삭제") {
            it("ADMIN 으로 호출하면 204 를 반환한다") {
                every {
                    useCase.perform(
                        match {
                            it.tagId.value == 42L &&
                                it.viewer.userId.value == 100L
                        }
                    )
                } just runs

                controller
                    .`when`(
                        delete("/v1/tags/{tagId}", 42).withAuth(role = SystemRole.ADMIN)
                    ).then(status().isNoContent)
                    .document(authHeaderRequired())

                verify(exactly = 1) {
                    useCase.perform(
                        match {
                            it.tagId.value == 42L &&
                                it.viewer.userId.value == 100L
                        }
                    )
                }
            }

            it("USER 가 호출하면 403 과 TAG_ADMIN_ONLY 를 반환한다") {
                every { useCase.perform(any()) } throws
                    ForbiddenException(TagErrorCode.TAG_ADMIN_ONLY)

                controller
                    .`when`(
                        delete("/v1/tags/{tagId}", 42).withAuth(role = SystemRole.USER)
                    ).then(
                        status().isForbidden,
                        jsonPath("$.code").value("TAG_ADMIN_ONLY"),
                        jsonPath("$.message").value("관리자만 태그를 삭제할 수 있습니다.")
                    )
            }

            it("Authorization 토큰이 없으면 401 을 반환한다") {
                every { useCase.perform(any()) } just runs

                controller
                    .`when`(delete("/v1/tags/{tagId}", 42))
                    .then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("tagId 형식이 숫자가 아니면 400 을 반환한다") {
                every { useCase.perform(any()) } just runs

                controller
                    .`when`(
                        delete("/v1/tags/{tagId}", "not-a-number").withAuth()
                    ).then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
