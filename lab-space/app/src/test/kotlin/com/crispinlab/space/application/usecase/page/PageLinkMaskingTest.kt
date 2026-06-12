package com.crispinlab.space.application.usecase.page

import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.application.port.outgoing.page.PageVisibilityRecord
import com.crispinlab.space.application.usecase.page.PageLinkMaskingPolicy.MASKED_DISPLAY_TEXT
import com.crispinlab.space.domain.page.PageContent
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.testsupport.TipTapJsonFixtures.doc
import com.crispinlab.space.testsupport.TipTapJsonFixtures.pageLink
import com.crispinlab.space.testsupport.TipTapJsonFixtures.paragraph
import com.crispinlab.space.testsupport.TipTapJsonFixtures.text
import com.crispinlab.user.domain.user.UserId
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain

class PageLinkMaskingTest :
    DescribeSpec({
        val mapper = ObjectMapper()
        val author = UserId(100L)
        val otherAuthor = UserId(200L)
        val space = SpaceId(10L)
        val otherSpace = SpaceId(20L)
        val anonymousScope: VisibilityScope = VisibilityScope.Anonymous
        val authorScope: VisibilityScope =
            VisibilityScope.Authenticated(
                viewerId = author,
                memberOfSpaceIds = setOf(space)
            )

        describe("maskPageLinksBy") {
            it("PUBLIC target 은 anonymous 에게도 그대로 노출된다") {
                val content =
                    singleLinkDoc(
                        pageId = 42L,
                        displayText = "문서"
                    )
                val visibilities =
                    mapOf(
                        PageId(42L) to
                            record(
                                pageId = PageId(42L),
                                visibility = Visibility.PUBLIC,
                                spaceId = space,
                                authorId = author
                            )
                    )

                val masked =
                    content.maskPageLinksBy(mapper, anonymousScope) { visibilities }

                masked.displayTextOf(
                    mapper = mapper,
                    targetPageId = 42L
                ) shouldBe "문서"
            }

            it("INTERNAL target 은 anonymous 에게 마스킹된다") {
                val content =
                    singleLinkDoc(
                        pageId = 42L,
                        displayText = "문서"
                    )
                val visibilities =
                    mapOf(
                        PageId(42L) to
                            record(
                                pageId = PageId(42L),
                                visibility = Visibility.INTERNAL,
                                spaceId = space,
                                authorId = author
                            )
                    )

                val masked =
                    content.maskPageLinksBy(mapper, anonymousScope) { visibilities }

                masked.displayTextOf(
                    mapper = mapper,
                    targetPageId = 42L
                ) shouldBe MASKED_DISPLAY_TEXT
                masked.pageIdAttrOf(
                    mapper = mapper,
                    targetPageId = 42L
                ) shouldBe "42"
            }

            it("DRAFT target 은 작성자 본인에게 노출된다") {
                val content =
                    singleLinkDoc(
                        pageId = 42L,
                        displayText = "문서"
                    )
                val visibilities =
                    mapOf(
                        PageId(42L) to
                            record(
                                pageId = PageId(42L),
                                visibility = Visibility.DRAFT,
                                spaceId = space,
                                authorId = author
                            )
                    )

                val masked =
                    content.maskPageLinksBy(mapper, authorScope) { visibilities }

                masked.displayTextOf(
                    mapper = mapper,
                    targetPageId = 42L
                ) shouldBe "문서"
            }

            it("member 는 자기 space 의 INTERNAL target 을 그대로 본다") {
                val content =
                    singleLinkDoc(
                        pageId = 42L,
                        displayText = "팀 자료"
                    )
                val visibilities =
                    mapOf(
                        PageId(42L) to
                            record(
                                pageId = PageId(42L),
                                visibility = Visibility.INTERNAL,
                                spaceId = space,
                                authorId = otherAuthor
                            )
                    )

                val masked =
                    content.maskPageLinksBy(mapper, authorScope) { visibilities }

                masked.displayTextOf(
                    mapper = mapper,
                    targetPageId = 42L
                ) shouldBe "팀 자료"
            }

            it("member 는 자기 멤버 아닌 space 의 INTERNAL target 은 마스킹된다") {
                val content =
                    singleLinkDoc(
                        pageId = 42L,
                        displayText = "다른 팀"
                    )
                val visibilities =
                    mapOf(
                        PageId(42L) to
                            record(
                                pageId = PageId(42L),
                                visibility = Visibility.INTERNAL,
                                spaceId = otherSpace,
                                authorId = otherAuthor
                            )
                    )

                val masked =
                    content.maskPageLinksBy(mapper, authorScope) { visibilities }

                masked.displayTextOf(
                    mapper = mapper,
                    targetPageId = 42L
                ) shouldBe MASKED_DISPLAY_TEXT
            }

            it("다른 사용자의 DRAFT target 은 member 에게 마스킹된다") {
                val content =
                    singleLinkDoc(
                        pageId = 42L,
                        displayText = "타인의 초안"
                    )
                val visibilities =
                    mapOf(
                        PageId(42L) to
                            record(
                                pageId = PageId(42L),
                                visibility = Visibility.DRAFT,
                                spaceId = space,
                                authorId = otherAuthor
                            )
                    )

                val masked =
                    content.maskPageLinksBy(mapper, authorScope) { visibilities }

                masked.displayTextOf(
                    mapper = mapper,
                    targetPageId = 42L
                ) shouldBe MASKED_DISPLAY_TEXT
            }

            it("visibilities map 에 없으면 (soft-deleted) 마스킹된다") {
                val content =
                    singleLinkDoc(
                        pageId = 42L,
                        displayText = "문서"
                    )

                val masked =
                    content.maskPageLinksBy(mapper, anonymousScope) { emptyMap() }

                masked.displayTextOf(
                    mapper = mapper,
                    targetPageId = 42L
                ) shouldBe MASKED_DISPLAY_TEXT
            }

            it("pageLink 가 없으면 lookup callback 호출 없이 원본 그대로 반환한다") {
                val content =
                    PageContent(doc(paragraph(text("그냥 평문입니다"))))
                var lookupCalled = false

                val masked =
                    content.maskPageLinksBy(mapper, anonymousScope) {
                        lookupCalled = true
                        emptyMap()
                    }

                masked.raw shouldBe content.raw
                lookupCalled shouldBe false
            }

            it("여러 pageLink 중 deny 만 마스킹된다") {
                val content =
                    PageContent(
                        doc(
                            paragraph(
                                pageLink(
                                    pageId = 1L,
                                    displayText = "공개"
                                ),
                                pageLink(
                                    pageId = 2L,
                                    displayText = "비공개"
                                ),
                                pageLink(
                                    pageId = 3L,
                                    displayText = "또공개"
                                )
                            )
                        )
                    )
                val visibilities =
                    mapOf(
                        PageId(1L) to
                            record(
                                pageId = PageId(1L),
                                visibility = Visibility.PUBLIC,
                                spaceId = space,
                                authorId = author
                            ),
                        PageId(2L) to
                            record(
                                pageId = PageId(2L),
                                visibility = Visibility.INTERNAL,
                                spaceId = space,
                                authorId = author
                            ),
                        PageId(3L) to
                            record(
                                pageId = PageId(3L),
                                visibility = Visibility.PUBLIC,
                                spaceId = space,
                                authorId = author
                            )
                    )

                val masked =
                    content.maskPageLinksBy(mapper, anonymousScope) { visibilities }

                masked.displayTextOf(
                    mapper = mapper,
                    targetPageId = 1L
                ) shouldBe "공개"
                masked.displayTextOf(
                    mapper = mapper,
                    targetPageId = 2L
                ) shouldBe MASKED_DISPLAY_TEXT
                masked.displayTextOf(
                    mapper = mapper,
                    targetPageId = 3L
                ) shouldBe "또공개"
            }

            it("마스킹 후에도 pageId 와 type 은 보존된다 (클릭 시 backend 404 흐름 유지)") {
                val content =
                    singleLinkDoc(
                        pageId = 42L,
                        displayText = "원본"
                    )
                val visibilities =
                    mapOf(
                        PageId(42L) to
                            record(
                                pageId = PageId(42L),
                                visibility = Visibility.INTERNAL,
                                spaceId = space,
                                authorId = author
                            )
                    )

                val masked =
                    content.maskPageLinksBy(mapper, anonymousScope) { visibilities }

                val node =
                    masked.findPageLinkNode(
                        mapper = mapper,
                        targetPageId = 42L
                    )
                node shouldNotBe null
                node!!["type"].asText() shouldBe "pageLink"
                node["attrs"]["pageId"].asText() shouldBe "42"
                node["attrs"]["displayText"].asText() shouldBe MASKED_DISPLAY_TEXT
            }

            it(
                "attrs 가 ObjectNode 가 아닌 corrupt pageLink 도 default-deny 로 마스킹된다"
            ) {
                val corruptPageLink = """{"type":"pageLink","attrs":[1,2,3]}"""
                val content =
                    PageContent(
                        doc(
                            paragraph(
                                corruptPageLink,
                                pageLink(
                                    pageId = 42L,
                                    displayText = "정상"
                                )
                            )
                        )
                    )
                val visibilities =
                    mapOf(
                        PageId(42L) to
                            record(
                                pageId = PageId(42L),
                                visibility = Visibility.INTERNAL,
                                spaceId = space,
                                authorId = author
                            )
                    )

                val masked =
                    content.maskPageLinksBy(mapper, anonymousScope) { visibilities }

                masked.displayTextOf(
                    mapper = mapper,
                    targetPageId = 42L
                ) shouldBe MASKED_DISPLAY_TEXT
                masked.raw shouldNotContain "[1,2,3]"
                val maskedCount: Int = masked.raw.split(MASKED_DISPLAY_TEXT).size - 1
                maskedCount shouldBe 2
            }
        }
    }) {
    companion object {
        fun record(
            pageId: PageId,
            visibility: Visibility,
            spaceId: SpaceId,
            authorId: UserId
        ): PageVisibilityRecord =
            PageVisibilityRecord(
                pageId = pageId,
                visibility = visibility,
                spaceId = spaceId,
                authorId = authorId
            )

        fun singleLinkDoc(
            pageId: Long,
            displayText: String?
        ): PageContent =
            PageContent(
                doc(
                    paragraph(
                        text("관련 "),
                        pageLink(
                            pageId = pageId,
                            displayText = displayText
                        ),
                        text(" 참고")
                    )
                )
            )

        fun PageContent.findPageLinkNode(
            mapper: ObjectMapper,
            targetPageId: Long
        ): JsonNode? = findInTree(mapper.readTree(raw), targetPageId)

        private fun findInTree(
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
                val found = findInTree(child, targetPageId)
                if (found != null) return found
            }
            return null
        }

        fun PageContent.displayTextOf(
            mapper: ObjectMapper,
            targetPageId: Long
        ): String? =
            findPageLinkNode(mapper, targetPageId)
                ?.get("attrs")
                ?.get("displayText")
                ?.takeIf { it.isTextual }
                ?.asText()

        fun PageContent.pageIdAttrOf(
            mapper: ObjectMapper,
            targetPageId: Long
        ): String? =
            findPageLinkNode(mapper, targetPageId)
                ?.get("attrs")
                ?.get("pageId")
                ?.asText()
    }
}
