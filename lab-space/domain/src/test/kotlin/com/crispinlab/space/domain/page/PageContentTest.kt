package com.crispinlab.space.domain.page

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
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

            it("최대 길이를 넘으면 거부한다") {
                shouldThrow<IllegalArgumentException> {
                    PageContent("x".repeat(PageContent.MAX_RAW_LENGTH + 1))
                }
            }

            it("정상 raw 값은 그대로 노출된다") {
                val raw: String = """{"type":"doc","content":[]}"""

                PageContent(raw).raw shouldBe raw
            }
        }
    })
