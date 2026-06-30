package com.crispinlab.space.domain.comment

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CommentContentTest :
    DescribeSpec({
        describe("init") {
            it("빈 본문은 거부한다") {
                shouldThrow<IllegalArgumentException> {
                    CommentContent("")
                }
            }

            it("공백만 있는 본문도 거부한다") {
                shouldThrow<IllegalArgumentException> {
                    CommentContent("   ")
                }
            }

            it("최대 길이를 넘으면 거부한다") {
                shouldThrow<IllegalArgumentException> {
                    CommentContent("x".repeat(CommentContent.MAX_RAW_LENGTH + 1))
                }
            }

            it("정상 raw 값은 그대로 노출된다") {
                val raw: String = """{"type":"doc","content":[]}"""

                CommentContent(raw).raw shouldBe raw
            }
        }
    })
