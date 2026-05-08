package com.crispinlab.space.domain.page

import com.crispinlab.space.domain.page.PageLink.Type.Companion.asType
import com.crispinlab.space.testsupport.Fixtures.basicPageLink
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class PageLinkTest :
    DescribeSpec({
        describe("init") {
            it("target 이 비어 있으면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicPageLink(target = "")
                }
            }
        }

        describe("Type.asType") {
            it("대소문자 무관하게 매칭한다") {
                "internal".asType() shouldBe PageLink.Type.INTERNAL
                "External".asType() shouldBe PageLink.Type.EXTERNAL
            }

            it("지원하지 않는 값이면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    "unknown".asType()
                }
            }
        }
    })
