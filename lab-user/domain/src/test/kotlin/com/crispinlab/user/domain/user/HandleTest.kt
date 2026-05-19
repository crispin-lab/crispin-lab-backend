package com.crispinlab.user.domain.user

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class HandleTest :
    DescribeSpec({
        describe("Handle") {
            it("영문 소문자, 숫자, 밑줄(_) 로 3~30자면 정상 생성된다") {
                Handle("abc").value shouldBe "abc"
                Handle("user_01").value shouldBe "user_01"
                Handle("a".repeat(Handle.MAX_LENGTH)).value shouldBe "a".repeat(Handle.MAX_LENGTH)
            }

            it("최소 길이 미만이면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    Handle("ab")
                }
            }

            it("최대 길이 초과면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    Handle("a".repeat(Handle.MAX_LENGTH + 1))
                }
            }

            it("빈 값이면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    Handle("")
                }
            }

            it("대문자가 포함되면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    Handle("UserName")
                }
            }

            it("허용되지 않는 특수문자가 포함되면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    Handle("user-name")
                }
                shouldThrow<IllegalArgumentException> {
                    Handle("user.name")
                }
                shouldThrow<IllegalArgumentException> {
                    Handle("user name")
                }
            }
        }
    })
