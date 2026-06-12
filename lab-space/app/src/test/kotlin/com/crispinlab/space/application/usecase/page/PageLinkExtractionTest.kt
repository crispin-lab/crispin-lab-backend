package com.crispinlab.space.application.usecase.page

import com.crispinlab.space.domain.page.ExtractedPageLink
import com.crispinlab.space.domain.page.PageContent
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.testsupport.TipTapJsonFixtures.bulletList
import com.crispinlab.space.testsupport.TipTapJsonFixtures.doc
import com.crispinlab.space.testsupport.TipTapJsonFixtures.listItem
import com.crispinlab.space.testsupport.TipTapJsonFixtures.pageLink
import com.crispinlab.space.testsupport.TipTapJsonFixtures.pageLinkWithRawAttrs
import com.crispinlab.space.testsupport.TipTapJsonFixtures.paragraph
import com.crispinlab.space.testsupport.TipTapJsonFixtures.text
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class PageLinkExtractionTest :
    DescribeSpec({
        val mapper = ObjectMapper()

        describe("extractPageLinks") {
            it("pageLink 가 없는 빈 doc 는 빈 리스트") {
                val content = PageContent("""{"type":"doc","content":[]}""")

                content.extractPageLinks(mapper).shouldBeEmpty()
            }

            it("paragraph 안의 단일 pageLink 노드를 추출한다") {
                val content =
                    PageContent(
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
                    )

                content.extractPageLinks(mapper) shouldContainExactly
                    listOf(
                        ExtractedPageLink(
                            targetPageId = PageId(42L),
                            displayText = "문서"
                        )
                    )
            }

            it("여러 paragraph 에 걸친 다중 pageLink 를 모두 추출한다") {
                val content =
                    PageContent(
                        doc(
                            paragraph(
                                pageLink(
                                    pageId = 1L,
                                    displayText = "첫번째"
                                )
                            ),
                            paragraph(
                                text("중간 "),
                                pageLink(
                                    pageId = 2L,
                                    displayText = "두번째"
                                )
                            )
                        )
                    )

                content.extractPageLinks(mapper).map { it.targetPageId } shouldBe
                    listOf(PageId(1L), PageId(2L))
            }

            it("displayText 가 null 이어도 PageId 만으로 추출된다") {
                val content =
                    PageContent(
                        doc(
                            paragraph(
                                pageLink(
                                    pageId = 7L,
                                    displayText = null
                                )
                            )
                        )
                    )

                content.extractPageLinks(mapper) shouldContainExactly
                    listOf(
                        ExtractedPageLink(
                            targetPageId = PageId(7L),
                            displayText = null
                        )
                    )
            }

            it("pageId 형식이 잘못된 pageLink 는 자연 drop 된다") {
                val content =
                    PageContent(
                        doc(
                            paragraph(
                                pageLinkWithRawAttrs(
                                    """"pageId":"not-a-number","displayText":"x""""
                                ),
                                pageLink(
                                    pageId = 99L,
                                    displayText = "정상"
                                )
                            )
                        )
                    )

                content.extractPageLinks(mapper).map { it.targetPageId } shouldBe
                    listOf(PageId(99L))
            }

            it("nested bulletList/listItem 안의 pageLink 도 깊이에 관계없이 추출한다") {
                val content =
                    PageContent(
                        doc(
                            bulletList(
                                listItem(
                                    paragraph(
                                        pageLink(
                                            pageId = 11L,
                                            displayText = "내부"
                                        )
                                    )
                                ),
                                listItem(
                                    paragraph(
                                        pageLink(
                                            pageId = 12L,
                                            displayText = "둘째"
                                        )
                                    )
                                )
                            )
                        )
                    )

                content.extractPageLinks(mapper).map { it.targetPageId } shouldBe
                    listOf(PageId(11L), PageId(12L))
            }

            it("pageLink 외 다른 노드와 혼재된 doc 는 pageLink 만 추출한다") {
                val content =
                    PageContent(
                        doc(
                            paragraph(text("앞 평문")),
                            paragraph(
                                pageLink(
                                    pageId = 3L,
                                    displayText = "추출 대상"
                                )
                            ),
                            paragraph(text("뒤 평문"))
                        )
                    )

                content.extractPageLinks(mapper) shouldContainExactly
                    listOf(
                        ExtractedPageLink(
                            targetPageId = PageId(3L),
                            displayText = "추출 대상"
                        )
                    )
            }

            it("malformed JSON 본문은 빈 리스트로 fallback") {
                val content = PageContent("not-json-at-all")

                content.extractPageLinks(mapper).shouldBeEmpty()
            }
        }
    })
