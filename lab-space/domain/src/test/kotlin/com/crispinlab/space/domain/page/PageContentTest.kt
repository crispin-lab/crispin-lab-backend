package com.crispinlab.space.domain.page

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

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
        }

        describe("extractLinks") {
            it("단일 [[페이지]] 를 INTERNAL 링크로 추출한다") {
                val content: PageContent = PageContent("관련 글 [[다른 페이지]] 참고")

                content.extractLinks() shouldContainExactly
                    listOf(ExtractedWikiLink(target = "다른 페이지", type = PageLink.Type.INTERNAL))
            }

            it("여러 링크를 본문 등장 순서대로 추출한다") {
                val content: PageContent =
                    PageContent("앞 [[첫 페이지]] 가운데 [[두번째]] 끝")

                val links: List<ExtractedWikiLink> = content.extractLinks()

                links shouldHaveSize 2
                links[0].target shouldBe "첫 페이지"
                links[1].target shouldBe "두번째"
            }

            it("https:// 로 시작하면 EXTERNAL 로 분류한다") {
                val content: PageContent =
                    PageContent("외부 [[https://example.com]] 참고")

                content.extractLinks() shouldContainExactly
                    listOf(
                        ExtractedWikiLink(
                            target = "https://example.com",
                            type = PageLink.Type.EXTERNAL
                        )
                    )
            }

            it("http:// 로 시작해도 EXTERNAL 로 분류한다") {
                val content: PageContent = PageContent("[[http://example.com]]")

                content.extractLinks().single().type shouldBe PageLink.Type.EXTERNAL
            }

            it("링크 안의 좌우 공백은 제거한다") {
                val content: PageContent = PageContent("[[  여백 페이지  ]]")

                content.extractLinks().single().target shouldBe "여백 페이지"
            }

            it("빈 [[]] 는 무시한다") {
                val content: PageContent = PageContent("내용 [[]] 사이")

                content.extractLinks() shouldHaveSize 0
            }

            it("링크가 없는 본문은 빈 리스트") {
                val content: PageContent = PageContent("그냥 평문입니다")

                content.extractLinks() shouldHaveSize 0
            }

            it("인접한 두 링크 [[A]][[B]] 를 각각 추출한다") {
                val content: PageContent = PageContent("[[첫번째]][[두번째]]")

                val links: List<ExtractedWikiLink> = content.extractLinks()

                links shouldHaveSize 2
                links[0].target shouldBe "첫번째"
                links[1].target shouldBe "두번째"
            }

            it("alias 표기 [[target|label]] 에서 target 만 추출한다") {
                val content: PageContent = PageContent("[[다른 페이지|예시 라벨]]")

                content.extractLinks().single().target shouldBe "다른 페이지"
            }

            it("alias 가 있는 외부 URL 도 target 으로 분류한다") {
                val content: PageContent = PageContent("[[https://example.com|예시]]")

                val link: ExtractedWikiLink = content.extractLinks().single()

                link.target shouldBe "https://example.com"
                link.type shouldBe PageLink.Type.EXTERNAL
            }
        }
    })
