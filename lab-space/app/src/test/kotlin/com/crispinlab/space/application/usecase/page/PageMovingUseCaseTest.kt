package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.exception.ConflictException
import com.crispinlab.common.exception.ForbiddenException
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.space.application.port.incoming.page.PageMoving.Request
import com.crispinlab.space.application.port.outgoing.page.PageAncestorPort
import com.crispinlab.space.application.port.outgoing.page.PageAncestorPort.Ancestor
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageErrorCode
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.space.testsupport.Fixtures.basicSpaceMember
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class PageMovingUseCaseTest :
    DescribeSpec({
        val pageRepository = mockk<PageRepository>()
        val pageAncestorPort = mockk<PageAncestorPort>()
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val useCase =
            PageMovingUseCase(
                pageRepository = pageRepository,
                pageAncestorPort = pageAncestorPort,
                spaceMemberRepository = spaceMemberRepository,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(pageRepository, pageAncestorPort, spaceMemberRepository)
            every {
                spaceMemberRepository.findBySpaceIdAndUserId(any(), any())
            } returns basicSpaceMember(role = SpaceMemberRole.MEMBER)
            every { pageRepository.save(any()) } answers { firstArg() }
            every { pageAncestorPort.findAncestorsOf(any()) } returns emptyList()
        }

        describe("페이지 부모 이동") {
            it("새 부모 scope 의 displayOrder MAX+1 을 entity 에 반영하고 저장한다") {
                val page = basicPage(parentPageId = PageId(50L))
                val newParent = basicPage(id = PageId(999L), spaceId = page.spaceId)
                every { pageRepository.findBy(page.id) } returns page
                every { pageRepository.findBy(newParent.id) } returns newParent
                every {
                    pageRepository.nextDisplayOrderIn(page.spaceId, newParent.id)
                } returns 7
                val savedPage = slot<Page>()
                every { pageRepository.save(capture(savedPage)) } answers { savedPage.captured }

                useCase.perform(
                    basicRequest(
                        pageId = page.id.value.toString(),
                        parentPageId = newParent.id.value.toString()
                    )
                )

                savedPage.captured.parentPageId shouldBe newParent.id
                savedPage.captured.displayOrder shouldBe 7
                verify(exactly = 1) {
                    pageRepository.nextDisplayOrderIn(page.spaceId, newParent.id)
                }
            }

            it("루트로 이동(null) 도 nextDisplayOrderIn(spaceId, null) 호출") {
                val page = basicPage(parentPageId = PageId(50L))
                every { pageRepository.findBy(page.id) } returns page
                every {
                    pageRepository.nextDisplayOrderIn(page.spaceId, null)
                } returns 3
                val savedPage = slot<Page>()
                every { pageRepository.save(capture(savedPage)) } answers { savedPage.captured }

                useCase.perform(
                    basicRequest(
                        pageId = page.id.value.toString(),
                        parentPageId = null
                    )
                )

                savedPage.captured.parentPageId shouldBe null
                savedPage.captured.displayOrder shouldBe 3
                verify(exactly = 0) { pageAncestorPort.findAncestorsOf(any()) }
            }

            it("같은 부모로 이동 요청 시 ConflictException") {
                val page = basicPage(parentPageId = PageId(50L))
                val sameParent = basicPage(id = PageId(50L), spaceId = page.spaceId)
                every { pageRepository.findBy(page.id) } returns page
                every { pageRepository.findBy(sameParent.id) } returns sameParent

                val exception =
                    shouldThrow<ConflictException> {
                        useCase.perform(
                            basicRequest(
                                pageId = page.id.value.toString(),
                                parentPageId = "50"
                            )
                        )
                    }

                exception.errorCode shouldBe PageErrorCode.PAGE_PARENT_UNCHANGED
                verify(exactly = 0) { pageRepository.save(any()) }
                verify(exactly = 0) { pageRepository.nextDisplayOrderIn(any(), any()) }
            }

            it("루트 → 루트 (null → null) 도 ConflictException(PAGE_PARENT_UNCHANGED)") {
                val page = basicPage(parentPageId = null)
                every { pageRepository.findBy(page.id) } returns page

                val exception =
                    shouldThrow<ConflictException> {
                        useCase.perform(
                            basicRequest(
                                pageId = page.id.value.toString(),
                                parentPageId = null
                            )
                        )
                    }

                exception.errorCode shouldBe PageErrorCode.PAGE_PARENT_UNCHANGED
                verify(exactly = 0) { pageRepository.save(any()) }
            }

            it("자기 자신을 부모로 지정하면 ConflictException(PAGE_PARENT_CYCLE)") {
                val page = basicPage(id = PageId(1L), parentPageId = null)
                every { pageRepository.findBy(page.id) } returns page

                val exception =
                    shouldThrow<ConflictException> {
                        useCase.perform(
                            basicRequest(
                                pageId = page.id.value.toString(),
                                parentPageId = page.id.value.toString()
                            )
                        )
                    }

                exception.errorCode shouldBe PageErrorCode.PAGE_PARENT_CYCLE
                verify(exactly = 0) { pageRepository.save(any()) }
                verify(exactly = 0) { pageAncestorPort.findAncestorsOf(any()) }
            }

            it("새 부모 page 가 존재하지 않으면 NotFoundException(PARENT_PAGE_NOT_FOUND)") {
                val page = basicPage(parentPageId = PageId(50L))
                every { pageRepository.findBy(page.id) } returns page
                every { pageRepository.findBy(PageId(999L)) } returns null

                val exception =
                    shouldThrow<NotFoundException> {
                        useCase.perform(
                            basicRequest(
                                pageId = page.id.value.toString(),
                                parentPageId = "999"
                            )
                        )
                    }

                exception.errorCode shouldBe PageErrorCode.PARENT_PAGE_NOT_FOUND
                verify(exactly = 0) { pageRepository.save(any()) }
            }

            it("새 부모 page 가 다른 space 면 NotFoundException(PARENT_PAGE_NOT_FOUND)") {
                val page = basicPage(parentPageId = PageId(50L), spaceId = SpaceId(10L))
                val otherSpaceParent =
                    basicPage(id = PageId(999L), spaceId = SpaceId(20L))
                every { pageRepository.findBy(page.id) } returns page
                every { pageRepository.findBy(otherSpaceParent.id) } returns otherSpaceParent

                val exception =
                    shouldThrow<NotFoundException> {
                        useCase.perform(
                            basicRequest(
                                pageId = page.id.value.toString(),
                                parentPageId = otherSpaceParent.id.value.toString()
                            )
                        )
                    }

                exception.errorCode shouldBe PageErrorCode.PARENT_PAGE_NOT_FOUND
                verify(exactly = 0) { pageRepository.save(any()) }
            }

            it("새 부모가 자기 자손이면 ConflictException(PAGE_PARENT_CYCLE)") {
                val page = basicPage(id = PageId(1L), parentPageId = null)
                val descendant = basicPage(id = PageId(50L), spaceId = page.spaceId)
                every { pageRepository.findBy(page.id) } returns page
                every { pageRepository.findBy(descendant.id) } returns descendant
                every { pageAncestorPort.findAncestorsOf(descendant.id) } returns
                    listOf(
                        Ancestor(
                            pageId = page.id,
                            title = page.title,
                            spaceId = page.spaceId,
                            spaceVisibility = SpaceVisibility.PUBLIC,
                            authorId = page.authorId,
                            visibility = Visibility.PUBLIC
                        )
                    )

                val exception =
                    shouldThrow<ConflictException> {
                        useCase.perform(
                            basicRequest(
                                pageId = page.id.value.toString(),
                                parentPageId = descendant.id.value.toString()
                            )
                        )
                    }

                exception.errorCode shouldBe PageErrorCode.PAGE_PARENT_CYCLE
                verify(exactly = 0) { pageRepository.save(any()) }
                verify(exactly = 0) { pageRepository.nextDisplayOrderIn(any(), any()) }
            }

            it("작성자가 아니면 NotFoundException (IDOR 통합)") {
                val page = basicPage(authorId = UserId(200L), parentPageId = null)
                every { pageRepository.findBy(page.id) } returns page

                shouldThrow<NotFoundException> {
                    useCase.perform(
                        basicRequest(
                            pageId = page.id.value.toString(),
                            parentPageId = "999",
                            userId = UserId(100L)
                        )
                    )
                }
                verify(exactly = 0) { pageRepository.save(any()) }
            }

            it("ADMIN 은 작성자가 아니어도 이동 가능하다") {
                val page = basicPage(authorId = UserId(200L), parentPageId = null)
                val newParent = basicPage(id = PageId(999L), spaceId = page.spaceId)
                every { pageRepository.findBy(page.id) } returns page
                every { pageRepository.findBy(newParent.id) } returns newParent
                every {
                    pageRepository.nextDisplayOrderIn(page.spaceId, newParent.id)
                } returns 4
                val savedPage = slot<Page>()
                every { pageRepository.save(capture(savedPage)) } answers { savedPage.captured }

                useCase.perform(
                    basicRequest(
                        pageId = page.id.value.toString(),
                        parentPageId = newParent.id.value.toString(),
                        userId = UserId(100L),
                        isAdmin = true
                    )
                )

                savedPage.captured.parentPageId shouldBe newParent.id
                savedPage.captured.displayOrder shouldBe 4
            }

            it("작성자라도 멤버에서 추방됐으면 ForbiddenException") {
                val page = basicPage(authorId = UserId(100L), parentPageId = null)
                every { pageRepository.findBy(page.id) } returns page
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(any(), any())
                } returns null

                shouldThrow<ForbiddenException> {
                    useCase.perform(
                        basicRequest(
                            pageId = page.id.value.toString(),
                            parentPageId = "999",
                            userId = UserId(100L)
                        )
                    )
                }
                verify(exactly = 0) { pageRepository.save(any()) }
            }

            it("페이지가 없으면 NotFoundException") {
                every { pageRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { pageRepository.save(any()) }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            pageId: String = "1",
            parentPageId: String? = null,
            userId: UserId = UserId(100L),
            isAdmin: Boolean = false
        ): Request =
            Request(
                pageId = pageId,
                parentPageId = parentPageId,
                viewer = Viewer.Member(userId = userId, isAdmin = isAdmin)
            )
    }
}
