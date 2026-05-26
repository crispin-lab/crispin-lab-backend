package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.space.application.port.incoming.page.PageGetting.Request
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.testsupport.DummyTransactionProvider
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class PageGettingUseCaseTest :
    DescribeSpec({
        val pageRepository = mockk<PageRepository>()
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val useCase =
            PageGettingUseCase(
                pageRepository = pageRepository,
                spaceMemberRepository = spaceMemberRepository,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(pageRepository, spaceMemberRepository)
            every { spaceMemberRepository.findSpaceIdsByUserId(any()) } returns emptySet()
        }

        describe("페이지 단건 조회") {
            it("정상적으로 조회한다 — author 본인 DRAFT") {
                val page = basicPage(title = "오늘의 회고")
                every { pageRepository.findBy(page.id) } returns page

                val result = useCase.perform(basicRequest(pageId = page.id.value.toString()))

                result.pageId shouldBe page.id
                result.title shouldBe "오늘의 회고"
                result.visibility shouldBe "DRAFT"
            }

            it("페이지가 없으면 NotFoundException") {
                every { pageRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
            }

            it("ID 형식이 올바르지 않으면 Request 생성에서 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(pageId = "not-a-number")
                }
            }

            it("비로그인 상태에서 PUBLIC 페이지는 조회 가능하다") {
                val page = basicPage(visibility = Visibility.PUBLIC)
                every { pageRepository.findBy(page.id) } returns page

                val result = useCase.perform(basicRequest(viewer = Viewer.Anonymous))

                result.pageId shouldBe page.id
            }

            it("비로그인 상태에서 INTERNAL 페이지는 NotFoundException 으로 응답한다") {
                val page = basicPage(visibility = Visibility.INTERNAL)
                every { pageRepository.findBy(page.id) } returns page

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(viewer = Viewer.Anonymous))
                }
            }

            it("멤버가 아닌 USER 가 INTERNAL 페이지를 보면 NotFoundException 으로 응답한다") {
                val page =
                    basicPage(spaceId = SpaceId(10L), visibility = Visibility.INTERNAL)
                every { pageRepository.findBy(page.id) } returns page
                every {
                    spaceMemberRepository.findSpaceIdsByUserId(UserId(100L))
                } returns emptySet()

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
            }

            it("멤버인 USER 는 INTERNAL 페이지를 조회 가능하다") {
                val page =
                    basicPage(spaceId = SpaceId(10L), visibility = Visibility.INTERNAL)
                every { pageRepository.findBy(page.id) } returns page
                every {
                    spaceMemberRepository.findSpaceIdsByUserId(UserId(100L))
                } returns setOf(SpaceId(10L))

                val result = useCase.perform(basicRequest())

                result.pageId shouldBe page.id
            }

            it("USER 가 다른 사용자의 DRAFT 페이지를 보면 NotFoundException 으로 응답한다") {
                val page = basicPage(authorId = UserId(200L), visibility = Visibility.DRAFT)
                every { pageRepository.findBy(page.id) } returns page

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
            }

            it("ADMIN 은 다른 사용자의 DRAFT 페이지도 조회 가능하다") {
                val page = basicPage(authorId = UserId(200L), visibility = Visibility.DRAFT)
                every { pageRepository.findBy(page.id) } returns page

                val result =
                    useCase.perform(
                        basicRequest(
                            viewer = Viewer.Member(userId = UserId(100L), isAdmin = true)
                        )
                    )

                result.pageId shouldBe page.id
            }
        }
    }) {
    companion object {
        fun basicRequest(
            pageId: String = "1",
            viewer: Viewer = Viewer.Member(userId = UserId(100L), isAdmin = false)
        ): Request =
            Request(
                pageId = pageId,
                viewer = viewer
            )
    }
}
