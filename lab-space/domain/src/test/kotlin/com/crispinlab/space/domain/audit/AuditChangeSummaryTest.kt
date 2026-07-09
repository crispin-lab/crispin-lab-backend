package com.crispinlab.space.domain.audit

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class AuditChangeSummaryTest :
    DescribeSpec({
        describe("init") {
            it("빈 값은 거부한다") {
                shouldThrow<IllegalArgumentException> {
                    AuditChangeSummary("")
                }
            }

            it("공백만 있는 값도 거부한다") {
                shouldThrow<IllegalArgumentException> {
                    AuditChangeSummary("   ")
                }
            }

            it("JSON 객체 형식이 아니면 거부한다") {
                shouldThrow<IllegalArgumentException> {
                    AuditChangeSummary("not-json")
                }
                shouldThrow<IllegalArgumentException> {
                    AuditChangeSummary("[1,2,3]")
                }
            }

            it("정상 JSON 문자열은 그대로 노출된다") {
                val raw: String = """{"name":{"before":"이전","after":"이후"}}"""

                AuditChangeSummary(raw).json shouldBe raw
            }
        }
    })
