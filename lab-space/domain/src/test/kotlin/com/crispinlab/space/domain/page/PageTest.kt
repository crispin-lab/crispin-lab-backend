package com.crispinlab.space.domain.page

import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.Fixtures.basicPage
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class PageTest :
    DescribeSpec({
        describe("init") {
            it("정상 생성") {
                val page: Page = basicPage()

                page.title shouldBe "초안"
                page.currentVersion shouldBe 1
                page.updatedAt shouldBe DUMMY_INSTANT
            }

            it("title 이 비어 있으면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicPage(title = "")
                }
            }

            it("title 이 ${Page.MAX_TITLE_LENGTH}자를 넘으면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicPage(title = "a".repeat(Page.MAX_TITLE_LENGTH + 1))
                }
            }

            it("parentPageId 가 자기 자신이면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicPage(id = PageId(1L), parentPageId = PageId(1L))
                }
            }
        }

        describe("update") {
            it("title, content, currentVersion(+1), updatedAt 가 모두 갱신된다") {
                val page: Page = basicPage()
                val occurredAt = DUMMY_INSTANT.plusSeconds(60)

                val result: Page.UpdateResult =
                    page.update(title = "수정 제목", content = "수정 본문", occurredAt = occurredAt)

                page.title shouldBe "수정 제목"
                page.content shouldBe PageContent("수정 본문")
                page.currentVersion shouldBe 2
                page.updatedAt shouldBe occurredAt
                result.version shouldBe 2
                result.occurredAt shouldBe occurredAt
            }

            it("연속 호출 시 currentVersion 이 누적 증가한다") {
                val page: Page = basicPage()

                page.update("t1", "c1", DUMMY_INSTANT.plusSeconds(1))
                page.update("t2", "c2", DUMMY_INSTANT.plusSeconds(2))
                page.update("t3", "c3", DUMMY_INSTANT.plusSeconds(3))

                page.currentVersion shouldBe 4
            }

            it("본문에서 위키 링크를 추출해 UpdateResult 에 담는다") {
                val page: Page = basicPage()

                val result: Page.UpdateResult =
                    page.update(
                        title = "제목",
                        content = "내부 [[다른 페이지]] 와 외부 [[https://example.com]]",
                        occurredAt = DUMMY_INSTANT.plusSeconds(60)
                    )

                result.wikiLinks shouldContainExactly
                    listOf(
                        ExtractedWikiLink(target = "다른 페이지", type = PageLink.Type.INTERNAL),
                        ExtractedWikiLink(
                            target = "https://example.com",
                            type = PageLink.Type.EXTERNAL
                        )
                    )
            }

            it("새 title 이 비어 있으면 실패한다") {
                val page: Page = basicPage()

                shouldThrow<IllegalArgumentException> {
                    page.update(
                        title = "",
                        content = "본문",
                        occurredAt = DUMMY_INSTANT.plusSeconds(60)
                    )
                }
            }
        }

        describe("move") {
            it("parentPageId 와 updatedAt 이 갱신된다") {
                val page: Page = basicPage()
                val occurredAt = DUMMY_INSTANT.plusSeconds(60)
                val newParent = PageId(999L)

                page.move(parentPageId = newParent, occurredAt = occurredAt)

                page.parentPageId shouldBe newParent
                page.updatedAt shouldBe occurredAt
            }

            it("루트로 이동(null) 도 정상 처리한다") {
                val page: Page =
                    basicPage().also {
                        it.move(
                            parentPageId = PageId(999L),
                            occurredAt = DUMMY_INSTANT.plusSeconds(1)
                        )
                    }

                page.move(parentPageId = null, occurredAt = DUMMY_INSTANT.plusSeconds(2))

                page.parentPageId shouldBe null
            }

            it("자기 자신으로 이동 시 실패한다") {
                val page: Page = basicPage()

                shouldThrow<IllegalArgumentException> {
                    page.move(parentPageId = page.id, occurredAt = DUMMY_INSTANT.plusSeconds(60))
                }
            }
        }

        describe("changeVisibility") {
            it("visibility 와 updatedAt 이 갱신된다") {
                val page: Page = basicPage()
                val occurredAt = DUMMY_INSTANT.plusSeconds(60)

                page.changeVisibility(visibility = Visibility.PUBLIC, occurredAt = occurredAt)

                page.visibility shouldBe Visibility.PUBLIC
                page.updatedAt shouldBe occurredAt
            }
        }
    })
