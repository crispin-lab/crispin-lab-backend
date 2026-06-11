package com.crispinlab.space.domain.page

import com.crispinlab.space.testsupport.Fixtures.basicPageLink
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.net.URI

class PageLinkTest :
    DescribeSpec({
        describe("Target") {
            it("Internal 은 targetPageId 를 노출한다") {
                val target =
                    basicPageLink(target = PageLink.Target.Internal(PageId(7L))).target

                target.shouldBeInstanceOf<PageLink.Target.Internal>()
                target.targetPageId shouldBe PageId(7L)
            }

            it("External 은 url 을 노출한다") {
                val target =
                    basicPageLink(
                        target = PageLink.Target.External(URI.create("https://example.com"))
                    ).target

                target.shouldBeInstanceOf<PageLink.Target.External>()
                target.url shouldBe URI.create("https://example.com")
            }

            it("External url 이 최대 길이를 넘으면 거부한다") {
                val tooLong = "https://example.com/" + "a".repeat(PageLink.MAX_EXTERNAL_URL_LENGTH)

                shouldThrow<IllegalArgumentException> {
                    PageLink.Target.External(URI.create(tooLong))
                }
            }
        }
    })
