package com.crispinlab.space.domain.page

import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.Fixtures.basicPage
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class PageTest :
    DescribeSpec({
        describe("생성") {
            it("정상 생성된다") {
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

            it("displayOrder 가 음수면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicPage(displayOrder = -1)
                }
            }

            it("displayOrder 디폴트는 0 이다") {
                basicPage().displayOrder shouldBe 0
            }
        }

        describe("본문 수정") {
            it("title·content·currentVersion(+1)·updatedAt 이 모두 갱신된다") {
                val page: Page = basicPage()

                val result: Page.EditResult = page.edit(title = "수정 제목", content = "수정 본문")

                page.title shouldBe "수정 제목"
                page.content shouldBe PageContent("수정 본문")
                page.currentVersion shouldBe 2
                page.updatedAt shouldNotBe DUMMY_INSTANT
                result.version shouldBe 2
            }

            it("연속 호출 시 currentVersion 이 누적 증가한다") {
                val page: Page = basicPage()

                page.edit("t1", "c1")
                page.edit("t2", "c2")
                page.edit("t3", "c3")

                page.currentVersion shouldBe 4
            }

            it("본문에서 위키 링크를 추출해 EditResult 에 담는다") {
                val page: Page = basicPage()

                val result: Page.EditResult =
                    page.edit(
                        title = "제목",
                        content = "내부 [[다른 페이지]] 와 외부 [[https://example.com]]"
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
                    page.edit(title = "", content = "본문")
                }
            }
        }

        describe("부모 이동") {
            it("parentPageId 와 updatedAt 이 갱신된다") {
                val page: Page = basicPage()
                val newParent = PageId(999L)

                page.move(parentPageId = newParent)

                page.parentPageId shouldBe newParent
                page.updatedAt shouldNotBe DUMMY_INSTANT
            }

            it("루트로 이동(null) 도 정상 처리한다") {
                val page: Page = basicPage().also { it.move(parentPageId = PageId(999L)) }

                page.move(parentPageId = null)

                page.parentPageId shouldBe null
            }

            it("자기 자신으로 이동 시 실패한다") {
                val page: Page = basicPage()

                shouldThrow<IllegalArgumentException> {
                    page.move(parentPageId = page.id)
                }
            }
        }

        describe("공개 범위 변경") {
            it("visibility 와 updatedAt 이 갱신된다") {
                val page: Page = basicPage()

                page.changeVisibility(visibility = Visibility.PUBLIC)

                page.visibility shouldBe Visibility.PUBLIC
                page.updatedAt shouldNotBe DUMMY_INSTANT
            }
        }

        describe("soft delete 상태 가드") {
            it("edit() 가 실패한다") {
                val page: Page = basicPage(deletedAt = DUMMY_INSTANT)

                shouldThrow<IllegalStateException> {
                    page.edit(title = "수정", content = "본문")
                }
            }

            it("move() 가 실패한다") {
                val page: Page = basicPage(deletedAt = DUMMY_INSTANT)

                shouldThrow<IllegalStateException> {
                    page.move(parentPageId = PageId(999L))
                }
            }

            it("changeVisibility() 가 실패한다") {
                val page: Page = basicPage(deletedAt = DUMMY_INSTANT)

                shouldThrow<IllegalStateException> {
                    page.changeVisibility(visibility = Visibility.PUBLIC)
                }
            }
        }
    })
