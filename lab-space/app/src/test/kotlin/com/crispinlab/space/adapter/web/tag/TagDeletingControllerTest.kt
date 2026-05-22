package com.crispinlab.space.adapter.web.tag

import com.crispinlab.space.application.port.incoming.tag.TagDeleting
import com.crispinlab.space.testsupport.SpaceAppControllerDescribeSpec
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
            it("성공하면 204 를 반환한다") {
                every {
                    useCase.perform(
                        match {
                            it.tagId.value == 42L &&
                                it.currentUserId.value == 100L
                        }
                    )
                } just runs

                controller
                    .`when`(
                        delete("/v1/tags/{tagId}", 42).withAuth()
                    ).then(status().isNoContent)
                    .document(authHeaderRequired())

                verify(exactly = 1) {
                    useCase.perform(
                        match {
                            it.tagId.value == 42L &&
                                it.currentUserId.value == 100L
                        }
                    )
                }
            }

            it("Authorization 토큰이 없으면 401 을 반환한다") {
                controller
                    .`when`(delete("/v1/tags/{tagId}", 42))
                    .then(
                        status().isUnauthorized,
                        jsonPath("$.code").value("INVALID_SESSION")
                    )
                verify(exactly = 0) { useCase.perform(any()) }
            }

            it("tagId 형식이 숫자가 아니면 400 을 반환한다") {
                controller
                    .`when`(
                        delete("/v1/tags/{tagId}", "not-a-number").withAuth()
                    ).then(status().isBadRequest)
                verify(exactly = 0) { useCase.perform(any()) }
            }
        }
    })
