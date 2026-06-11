package com.crispinlab.space.domain.page

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.net.URI

class PageContentTest :
    DescribeSpec({
        describe("init") {
            it("빈 본문은 거부한다") {
                shouldThrow<IllegalArgumentException> {
                    PageContent("")
                }
            }

            it("공백만 있는 본문도 거부한다") {
                shouldThrow<IllegalArgumentException> {
                    PageContent("   ")
                }
            }

            it("최대 길이를 넘으면 거부한다") {
                shouldThrow<IllegalArgumentException> {
                    PageContent("x".repeat(PageContent.MAX_RAW_LENGTH + 1))
                }
            }
        }

        describe("extractLinks") {
            it("[[pageId:N]] 형태를 Internal 링크로 추출한다") {
                val content: PageContent = PageContent("관련 글 [[pageId:42]] 참고")

                content.extractLinks() shouldContainExactly
                    listOf(
                        ExtractedWikiLink.Internal(
                            targetPageId = PageId(42L),
                            displayText = null
                        )
                    )
            }

            it("[[pageId:N|displayText]] 의 displayText 를 추출한다") {
                val content: PageContent = PageContent("[[pageId:1|구조 설명]]")

                val link: ExtractedWikiLink = content.extractLinks().single()
                link.shouldBeInstanceOf<ExtractedWikiLink.Internal>()
                link.targetPageId shouldBe PageId(1L)
                link.displayText shouldBe "구조 설명"
            }

            it("여러 링크를 본문 등장 순서대로 추출한다") {
                val content: PageContent =
                    PageContent("앞 [[pageId:1|첫]] 가운데 [[pageId:2|두번째]] 끝")

                val links: List<ExtractedWikiLink> = content.extractLinks()

                links shouldHaveSize 2
                links[0].displayText shouldBe "첫"
                links[1].displayText shouldBe "두번째"
            }

            it("https:// 로 시작하면 External 로 분류한다") {
                val content: PageContent =
                    PageContent("외부 [[https://example.com|예시]] 참고")

                val link: ExtractedWikiLink = content.extractLinks().single()
                link.shouldBeInstanceOf<ExtractedWikiLink.External>()
                link.url shouldBe URI.create("https://example.com")
                link.displayText shouldBe "예시"
            }

            it("http:// 로 시작해도 External 로 분류한다") {
                val content: PageContent = PageContent("[[http://example.com]]")

                content.extractLinks().single().shouldBeInstanceOf<ExtractedWikiLink.External>()
            }

            it("인접한 두 internal 링크 [[pageId:1]][[pageId:2]] 를 각각 추출한다") {
                val content: PageContent = PageContent("[[pageId:1]][[pageId:2]]")

                val links: List<ExtractedWikiLink> = content.extractLinks()

                links shouldHaveSize 2
                (links[0] as ExtractedWikiLink.Internal).targetPageId shouldBe PageId(1L)
                (links[1] as ExtractedWikiLink.Internal).targetPageId shouldBe PageId(2L)
            }

            it("링크가 없는 본문은 빈 리스트") {
                val content: PageContent = PageContent("그냥 평문입니다")

                content.extractLinks() shouldHaveSize 0
            }

            it("이전 [[title]] syntax 는 더 이상 매칭되지 않는다") {
                val content: PageContent = PageContent("[[다른 페이지]] 와 [[제목|라벨]]")

                content.extractLinks() shouldHaveSize 0
            }

            it("internal 과 external 이 섞인 본문도 정상 처리한다") {
                val content: PageContent =
                    PageContent("[[pageId:5|문서]] 와 [[https://example.com|예시]]")

                val links: List<ExtractedWikiLink> = content.extractLinks()

                links shouldHaveSize 2
                links[0].shouldBeInstanceOf<ExtractedWikiLink.Internal>()
                links[1].shouldBeInstanceOf<ExtractedWikiLink.External>()
            }

            it("Long 범위를 넘는 pageId 는 추출 결과에서 누락된다") {
                val content: PageContent =
                    PageContent("[[pageId:99999999999999999999|문서]]")

                content.extractLinks() shouldHaveSize 0
            }

            it("닫는 ]] 가 없는 미완성 토큰은 매칭되지 않는다") {
                val content: PageContent = PageContent("[[pageId:1 본문 중간")

                content.extractLinks() shouldHaveSize 0
            }

            it("`|` 뒤가 비어 있는 토큰은 매칭되지 않는다") {
                val content: PageContent = PageContent("[[pageId:1|]]")

                content.extractLinks() shouldHaveSize 0
            }

            it("공백이 포함된 URL 토큰은 정규식에서 거부되어 누락된다") {
                val content: PageContent = PageContent("[[https://has space|예시]]")

                content.extractLinks() shouldHaveSize 0
            }

            it("regex 가 통과시킨 malformed URL 은 URI.create 실패로 누락된다") {
                val content: PageContent = PageContent("[[https://%ZZ|예시]]")

                content.extractLinks() shouldHaveSize 0
            }

            it("매우 긴 displayText 도 정상 추출한다") {
                val longDisplay: String = "x".repeat(10_000)
                val content: PageContent = PageContent("[[pageId:1|$longDisplay]]")

                val link = content.extractLinks().single()
                link.shouldBeInstanceOf<ExtractedWikiLink.Internal>()
                link.displayText shouldBe longDisplay
            }
        }
    })
