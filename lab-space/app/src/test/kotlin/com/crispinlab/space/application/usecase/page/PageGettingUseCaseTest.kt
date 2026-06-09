package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.space.application.port.incoming.page.PageGetting.Request
import com.crispinlab.space.application.port.outgoing.page.PageAncestorPort
import com.crispinlab.space.application.port.outgoing.page.PageAncestorPort.Ancestor
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class PageGettingUseCaseTest :
    DescribeSpec({
        val pageRepository = mockk<PageRepository>()
        val pageAncestorPort = mockk<PageAncestorPort>()
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val useCase =
            PageGettingUseCase(
                pageRepository = pageRepository,
                pageAncestorPort = pageAncestorPort,
                spaceMemberRepository = spaceMemberRepository,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(pageRepository, pageAncestorPort, spaceMemberRepository)
            every { spaceMemberRepository.findSpaceIdsByUserId(any()) } returns emptySet()
            every { pageAncestorPort.findAncestorsOf(any()) } returns emptyList()
        }

        describe("페이지 단건 조회") {
            it("정상적으로 조회한다 — author 본인 DRAFT") {
                val page = basicPage(title = "오늘의 회고")
                every { pageRepository.findBy(page.id) } returns page

                val result = useCase.perform(basicRequest(pageId = page.id.value.toString()))

                result.pageId shouldBe page.id
                result.title shouldBe "오늘의 회고"
                result.visibility shouldBe "DRAFT"
                result.ancestors shouldBe emptyList()
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

            it("Result.displayOrder 가 entity 의 값을 그대로 노출한다") {
                val page = basicPage(displayOrder = 4)
                every { pageRepository.findBy(page.id) } returns page

                val result = useCase.perform(basicRequest(pageId = page.id.value.toString()))

                result.displayOrder shouldBe 4
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

        describe("ancestors 응답") {
            it("3단계 체인이면 root → 직계 부모 순서로 응답한다") {
                val page = basicPage(id = PageId(3L), parentPageId = PageId(2L))
                every { pageRepository.findBy(page.id) } returns page
                every { pageAncestorPort.findAncestorsOf(page.id) } returns
                    listOf(
                        publicAncestor(pageId = PageId(1L), title = "root"),
                        publicAncestor(pageId = PageId(2L), title = "mid")
                    )

                val result = useCase.perform(basicRequest(pageId = page.id.value.toString()))

                result.ancestors shouldHaveSize 2
                result.ancestors[0].pageId shouldBe PageId(1L)
                result.ancestors[0].title shouldBe "root"
                result.ancestors[1].pageId shouldBe PageId(2L)
                result.ancestors[1].title shouldBe "mid"
            }

            it("정책에 걸리는 중간 ancestor 는 응답에서 빠지고 순서는 유지된다") {
                val page = basicPage(id = PageId(4L), parentPageId = PageId(3L))
                every { pageRepository.findBy(page.id) } returns page
                every { pageAncestorPort.findAncestorsOf(page.id) } returns
                    listOf(
                        publicAncestor(pageId = PageId(1L), title = "root"),
                        Ancestor(
                            pageId = PageId(2L),
                            title = "타인의 초안",
                            spaceId = SpaceId(10L),
                            authorId = UserId(200L),
                            visibility = Visibility.DRAFT
                        ),
                        publicAncestor(pageId = PageId(3L), title = "직계 부모")
                    )

                val result = useCase.perform(basicRequest(pageId = page.id.value.toString()))

                result.ancestors shouldHaveSize 2
                result.ancestors.map { it.pageId } shouldBe listOf(PageId(1L), PageId(3L))
            }

            it("ADMIN 에게는 DRAFT ancestor 도 마스킹되지 않는다") {
                val page = basicPage(id = PageId(5L), parentPageId = PageId(2L))
                every { pageRepository.findBy(page.id) } returns page
                every { pageAncestorPort.findAncestorsOf(page.id) } returns
                    listOf(
                        Ancestor(
                            pageId = PageId(2L),
                            title = "타인의 초안",
                            spaceId = SpaceId(10L),
                            authorId = UserId(200L),
                            visibility = Visibility.DRAFT
                        )
                    )

                val result =
                    useCase.perform(
                        basicRequest(
                            pageId = page.id.value.toString(),
                            viewer = Viewer.Member(userId = UserId(100L), isAdmin = true)
                        )
                    )

                result.ancestors shouldHaveSize 1
                result.ancestors[0].pageId shouldBe PageId(2L)
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

        private fun publicAncestor(
            pageId: PageId,
            title: String
        ): Ancestor =
            Ancestor(
                pageId = pageId,
                title = title,
                spaceId = SpaceId(10L),
                authorId = UserId(100L),
                visibility = Visibility.PUBLIC
            )
    }
}
