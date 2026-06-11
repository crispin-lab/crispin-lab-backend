package com.crispinlab.space.domain.page

import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.application.port.outgoing.page.PageVisibilityRecord
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.domain.user.UserId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class PageLinkMaskingTest :
    DescribeSpec({
        val author = UserId(100L)
        val space = SpaceId(10L)
        val anonymousScope: VisibilityScope = VisibilityScope.Anonymous
        val authorScope: VisibilityScope =
            VisibilityScope.Authenticated(viewerId = author, memberOfSpaceIds = setOf(space))

        describe("maskPageLinks") {
            it("PUBLIC target 은 anonymous 에게도 그대로 노출된다") {
                val content = PageContent("관련 [[pageId:42|문서]] 참고")
                val visibilities =
                    mapOf(PageId(42L) to record(PageId(42L), Visibility.PUBLIC, space, author))

                val masked = content.maskPageLinks(anonymousScope, visibilities)

                masked.raw shouldBe "관련 [[pageId:42|문서]] 참고"
            }

            it("INTERNAL target 은 anonymous 에게 마스킹된다") {
                val content = PageContent("관련 [[pageId:42|문서]] 참고")
                val visibilities =
                    mapOf(
                        PageId(42L) to record(PageId(42L), Visibility.INTERNAL, space, author)
                    )

                val masked = content.maskPageLinks(anonymousScope, visibilities)

                masked.raw shouldBe "관련 비공개 페이지 참고"
            }

            it("DRAFT target 은 작성자 본인에게 노출된다") {
                val content = PageContent("관련 [[pageId:42|문서]] 참고")
                val visibilities =
                    mapOf(PageId(42L) to record(PageId(42L), Visibility.DRAFT, space, author))

                val masked = content.maskPageLinks(authorScope, visibilities)

                masked.raw shouldBe "관련 [[pageId:42|문서]] 참고"
            }

            it("visibilities map 에 없으면 (soft-deleted) 마스킹된다") {
                val content = PageContent("관련 [[pageId:42|문서]] 참고")

                val masked = content.maskPageLinks(anonymousScope, emptyMap())

                masked.raw shouldBe "관련 비공개 페이지 참고"
            }

            it("Internal 매치 0개인 본문은 원본 그대로 반환한다") {
                val content = PageContent("그냥 평문입니다")

                val masked = content.maskPageLinks(anonymousScope, emptyMap())

                masked.raw shouldBe "그냥 평문입니다"
            }

            it("External 링크는 마스킹 대상이 아니다") {
                val content =
                    PageContent("[[https://example.com|예시]] 와 [[pageId:42|문서]]")
                val visibilities =
                    mapOf(
                        PageId(42L) to record(PageId(42L), Visibility.INTERNAL, space, author)
                    )

                val masked = content.maskPageLinks(anonymousScope, visibilities)

                masked.raw shouldBe "[[https://example.com|예시]] 와 비공개 페이지"
            }

            it("여러 internal 매치 중 deny 만 마스킹된다") {
                val content =
                    PageContent("[[pageId:1|공개]] / [[pageId:2|비공개]] / [[pageId:3|또공개]]")
                val visibilities =
                    mapOf(
                        PageId(1L) to record(PageId(1L), Visibility.PUBLIC, space, author),
                        PageId(2L) to record(PageId(2L), Visibility.INTERNAL, space, author),
                        PageId(3L) to record(PageId(3L), Visibility.PUBLIC, space, author)
                    )

                val masked = content.maskPageLinks(anonymousScope, visibilities)

                masked.raw shouldBe
                    "[[pageId:1|공개]] / 비공개 페이지 / [[pageId:3|또공개]]"
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
    }
}
