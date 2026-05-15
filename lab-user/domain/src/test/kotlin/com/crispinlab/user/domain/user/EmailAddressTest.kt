package com.crispinlab.user.domain.user

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class EmailAddressTest :
    DescribeSpec({
        describe("생성") {
            it("정상 형식이면 그대로 보관한다") {
                val email: EmailAddress = EmailAddress("user@example.com")

                email.value shouldBe "user@example.com"
            }

            it("빈 값이면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    EmailAddress("")
                }
            }

            it("@ 가 없으면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    EmailAddress("userexample.com")
                }
            }

            it("도메인 TLD 가 없으면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    EmailAddress("user@example")
                }
            }

            it("공백이 포함되면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    EmailAddress("user name@example.com")
                }
            }

            it("${EmailAddress.MAX_LENGTH}자를 넘으면 실패한다") {
                val domain = "@example.com"
                val local: String = "a".repeat(EmailAddress.MAX_LENGTH - domain.length + 1)

                shouldThrow<IllegalArgumentException> {
                    EmailAddress("$local$domain")
                }
            }
        }
    })
