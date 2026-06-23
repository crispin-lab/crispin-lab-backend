package com.crispinlab.user.domain.credential

import com.crispinlab.user.domain.credential.Password.Outcome
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class PasswordTest :
    DescribeSpec({
        describe("길이") {
            it("7자는 TooShort") {
                Password.parse("aB1!aB1") shouldBe Outcome.TooShort
            }

            it("8자는 Ok") {
                val outcome = Password.parse("aB1!aB1!").shouldBeInstanceOf<Outcome.Ok>()
                outcome.password.raw shouldBe "aB1!aB1!"
            }

            it("72자는 Ok") {
                val raw = "aB1!" + "a".repeat(68)
                val outcome = Password.parse(raw).shouldBeInstanceOf<Outcome.Ok>()
                outcome.password.raw shouldBe raw
            }

            it("73자는 TooLong") {
                val raw = "aB1!" + "a".repeat(69)
                Password.parse(raw) shouldBe Outcome.TooLong
            }
        }

        describe("문자종 다양성") {
            it("영문만 → InsufficientVariety") {
                Password.parse("abcdefgh") shouldBe Outcome.InsufficientVariety
            }

            it("숫자만 → InsufficientVariety") {
                Password.parse("12345678") shouldBe Outcome.InsufficientVariety
            }

            it("특수문자만 → InsufficientVariety") {
                Password.parse("!@#\$%^&*") shouldBe Outcome.InsufficientVariety
            }

            it("영문+숫자 → Ok") {
                Password.parse("abcd1234").shouldBeInstanceOf<Outcome.Ok>()
            }

            it("영문+특수문자 → Ok") {
                Password.parse("abcd!@#\$").shouldBeInstanceOf<Outcome.Ok>()
            }

            it("숫자+특수문자 → Ok") {
                Password.parse("1234!@#\$").shouldBeInstanceOf<Outcome.Ok>()
            }

            it("한글은 특수문자 종으로 카운트된다") {
                Password.parse("abcd한글입니").shouldBeInstanceOf<Outcome.Ok>()
            }

            it("한글만 → InsufficientVariety") {
                Password.parse("한글비밀번호다양") shouldBe Outcome.InsufficientVariety
            }
        }

        describe("공백 정책") {
            it("앞쪽 공백 → ContainsWhitespace") {
                Password.parse(" abcd1234") shouldBe Outcome.ContainsWhitespace
            }

            it("뒤쪽 공백 → ContainsWhitespace") {
                Password.parse("abcd1234 ") shouldBe Outcome.ContainsWhitespace
            }

            it("탭/개행 같은 화이트스페이스도 양끝이면 차단") {
                Password.parse("\tabcd1234") shouldBe Outcome.ContainsWhitespace
                Password.parse("abcd1234\n") shouldBe Outcome.ContainsWhitespace
            }

            it("가운데 공백은 허용한다") {
                Password.parse("abcd 1234").shouldBeInstanceOf<Outcome.Ok>()
            }

            it("빈 문자열은 TooShort 로 떨어진다 (양끝 공백 메시지 회피)") {
                Password.parse("") shouldBe Outcome.TooShort
            }
        }

        describe("위반 우선순위") {
            it("양끝 공백이 길이/문자종보다 먼저 노출된다") {
                Password.parse(" abc") shouldBe Outcome.ContainsWhitespace
            }

            it("길이 부족이 문자종보다 먼저 노출된다") {
                Password.parse("abc") shouldBe Outcome.TooShort
            }
        }

        describe("Violation 의 errorCode 매핑") {
            it("각 Violation 이 PasswordErrorCode 와 1:1 매핑된다") {
                Outcome.TooShort.errorCode shouldBe PasswordErrorCode.PASSWORD_TOO_SHORT
                Outcome.TooLong.errorCode shouldBe PasswordErrorCode.PASSWORD_TOO_LONG
                Outcome.ContainsWhitespace.errorCode shouldBe
                    PasswordErrorCode.PASSWORD_CONTAINS_WHITESPACE
                Outcome.InsufficientVariety.errorCode shouldBe
                    PasswordErrorCode.PASSWORD_INSUFFICIENT_VARIETY
            }
        }

        describe("toString 마스킹") {
            it("toString 에 raw 값을 노출하지 않는다") {
                val password =
                    Password.parse("abcd1234").shouldBeInstanceOf<Outcome.Ok>().password
                password.toString() shouldBe "Password(raw=***)"
            }
        }
    })
