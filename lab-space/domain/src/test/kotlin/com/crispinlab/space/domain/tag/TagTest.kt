package com.crispinlab.space.domain.tag

import com.crispinlab.space.testsupport.Fixtures.basicTag
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class TagTest :
    DescribeSpec({
        describe("init") {
            it("정상 생성") {
                val tag: Tag = basicTag()

                tag.name shouldBe "kotlin"
            }

            it("한글 이름도 허용한다") {
                val tag: Tag = basicTag(name = "코틀린")

                tag.name shouldBe "코틀린"
            }

            it("공백 포함 이름은 거부한다") {
                shouldThrow<IllegalArgumentException> {
                    basicTag(name = "kotlin 4")
                }
            }

            it("빈 이름은 거부한다") {
                shouldThrow<IllegalArgumentException> {
                    basicTag(name = "")
                }
            }
        }

        describe("rename") {
            it("이름이 갱신된다") {
                val tag: Tag = basicTag()

                tag.rename("scala")

                tag.name shouldBe "scala"
            }

            it("새 이름이 형식에 맞지 않으면 실패한다") {
                val tag: Tag = basicTag()

                shouldThrow<IllegalArgumentException> {
                    tag.rename("invalid name with space")
                }
            }
        }
    })
