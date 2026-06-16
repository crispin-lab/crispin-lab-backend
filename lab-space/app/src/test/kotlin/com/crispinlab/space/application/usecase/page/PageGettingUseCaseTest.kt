package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.space.application.port.incoming.page.PageGetting.Request
import com.crispinlab.space.application.port.outgoing.page.PageAncestorPort
import com.crispinlab.space.application.port.outgoing.page.PageAncestorPort.Ancestor
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.page.PageVisibilityRecord
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.page.PageLinkMaskingPolicy.MASKED_DISPLAY_TEXT
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageContent
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.space.testsupport.TipTapJsonFixtures.doc
import com.crispinlab.space.testsupport.TipTapJsonFixtures.pageLink
import com.crispinlab.space.testsupport.TipTapJsonFixtures.paragraph
import com.crispinlab.space.testsupport.TipTapJsonFixtures.text
import com.crispinlab.user.application.port.outgoing.user.UserHandleQuery
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.UserId
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class PageGettingUseCaseTest :
    DescribeSpec({
        val pageRepository = mockk<PageRepository>()
        val pageAncestorPort = mockk<PageAncestorPort>()
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val userHandleQuery = mockk<UserHandleQuery>()
        val objectMapper = ObjectMapper()
        val useCase =
            PageGettingUseCase(
                pageRepository = pageRepository,
                pageAncestorPort = pageAncestorPort,
                spaceMemberRepository = spaceMemberRepository,
                userHandleQuery = userHandleQuery,
                transactionProvider = DummyTransactionProvider(),
                objectMapper = objectMapper
            )

        beforeEach {
            clearMocks(pageRepository, pageAncestorPort, spaceMemberRepository, userHandleQuery)
            every { spaceMemberRepository.findSpaceIdsByUserId(any()) } returns emptySet()
            every { pageAncestorPort.findAncestorsOf(any()) } returns emptyList()
            every { pageRepository.findVisibilitiesByIds(any()) } returns emptyMap()
            every { userHandleQuery.handlesOf(any()) } returns
                mapOf(UserId(100L) to Handle("test_user"))
        }

        describe("페이지 단건 조회") {
            it("정상적으로 조회한다 — author 본인 DRAFT") {
                val page = basicPage(title = "오늘의 회고")
                every { pageRepository.findBy(page.id) } returns page

                val result = useCase.perform(basicRequest(pageId = page.id.value.toString()))

                result.pageId shouldBe page.id
                result.title shouldBe "오늘의 회고"
                result.visibility shouldBe Visibility.DRAFT
                result.authorId shouldBe UserId(100L)
                result.authorHandle shouldBe "test_user"
                result.ancestors shouldBe emptyList()
                verify(exactly = 1) { userHandleQuery.handlesOf(setOf(UserId(100L))) }
            }

            it("author 가 삭제된 사용자라 handle 조회가 비면 authorHandle 은 빈 문자열로 응답한다") {
                val page = basicPage()
                every { pageRepository.findBy(page.id) } returns page
                every { userHandleQuery.handlesOf(any()) } returns emptyMap()

                val result = useCase.perform(basicRequest(pageId = page.id.value.toString()))

                result.authorId shouldBe UserId(100L)
                result.authorHandle shouldBe ""
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

            it("비로그인 상태에서 DRAFT 페이지는 NotFoundException 으로 응답한다") {
                val page = basicPage(visibility = Visibility.DRAFT)
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

        describe("content 의 PageLink displayText 마스킹") {
            val tipTapWithLink: String =
                doc(
                    paragraph(
                        text("관련 "),
                        pageLink(
                            pageId = 42L,
                            displayText = "문서"
                        ),
                        text(" 참고")
                    )
                )

            it("anonymous 는 PRIVATE target 의 매치를 마스킹된 텍스트로 본다") {
                val page =
                    basicPage(
                        visibility = Visibility.PUBLIC,
                        content = PageContent(tipTapWithLink)
                    )
                every { pageRepository.findBy(page.id) } returns page
                every { pageRepository.findVisibilitiesByIds(setOf(PageId(42L))) } returns
                    mapOf(
                        PageId(42L) to
                            PageVisibilityRecord(
                                pageId = PageId(42L),
                                visibility = Visibility.INTERNAL,
                                spaceId = SpaceId(10L),
                                authorId = UserId(200L)
                            )
                    )

                val result =
                    useCase.perform(basicRequest(viewer = Viewer.Anonymous))

                displayTextOf(
                    mapper = objectMapper,
                    json = result.content,
                    targetPageId = 42L
                ) shouldBe MASKED_DISPLAY_TEXT
            }

            it("target 이 PUBLIC 이면 anonymous 에게도 그대로 노출된다") {
                val page =
                    basicPage(
                        visibility = Visibility.PUBLIC,
                        content = PageContent(tipTapWithLink)
                    )
                every { pageRepository.findBy(page.id) } returns page
                every { pageRepository.findVisibilitiesByIds(setOf(PageId(42L))) } returns
                    mapOf(
                        PageId(42L) to
                            PageVisibilityRecord(
                                pageId = PageId(42L),
                                visibility = Visibility.PUBLIC,
                                spaceId = SpaceId(10L),
                                authorId = UserId(200L)
                            )
                    )

                val result =
                    useCase.perform(basicRequest(viewer = Viewer.Anonymous))

                displayTextOf(
                    mapper = objectMapper,
                    json = result.content,
                    targetPageId = 42L
                ) shouldBe "문서"
            }

            it("content 에 PageLink 가 없으면 visibility lookup 자체를 건너뛴다") {
                val plainDoc: String = doc(paragraph(text("그냥 평문")))
                val page = basicPage(content = PageContent(plainDoc))
                every { pageRepository.findBy(page.id) } returns page

                val result = useCase.perform(basicRequest())

                result.content shouldBe plainDoc
                verify(exactly = 0) { pageRepository.findVisibilitiesByIds(any()) }
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

        fun displayTextOf(
            mapper: ObjectMapper,
            json: String,
            targetPageId: Long
        ): String? =
            findPageLinkNode(mapper.readTree(json), targetPageId)
                ?.get("attrs")
                ?.get("displayText")
                ?.takeIf { it.isTextual }
                ?.asText()

        private fun findPageLinkNode(
            node: JsonNode,
            targetPageId: Long
        ): JsonNode? {
            if (node.isObject &&
                node["type"]?.asText() == "pageLink" &&
                node["attrs"]?.get("pageId")?.asText() == targetPageId.toString()
            ) {
                return node
            }
            for (child in node.elements()) {
                val found = findPageLinkNode(child, targetPageId)
                if (found != null) return found
            }
            return null
        }
    }
}
