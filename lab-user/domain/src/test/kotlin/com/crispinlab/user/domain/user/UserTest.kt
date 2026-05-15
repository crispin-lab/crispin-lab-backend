package com.crispinlab.user.domain.user

import com.crispinlab.user.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.user.testsupport.Fixtures.basicUser
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class UserTest :
    DescribeSpec({
        describe("생성") {
            it("정상 생성된다") {
                val user: User = basicUser()

                user.displayName shouldBe "테스트 사용자"
                user.updatedAt shouldBe DUMMY_INSTANT
            }

            it("표시 이름이 비어 있으면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicUser(displayName = "")
                }
            }

            it("표시 이름이 ${User.MAX_DISPLAY_NAME_LENGTH}자를 넘으면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicUser(displayName = "가".repeat(User.MAX_DISPLAY_NAME_LENGTH + 1))
                }
            }
        }

        describe("표시 이름 수정") {
            it("변경 시 displayName 과 updatedAt 이 갱신된다") {
                val user: User = basicUser()

                user.edit(displayName = "변경된 이름")

                user.displayName shouldBe "변경된 이름"
                user.updatedAt shouldNotBe DUMMY_INSTANT
            }

            it("새 표시 이름이 비어 있으면 실패한다") {
                val user: User = basicUser()

                shouldThrow<IllegalArgumentException> {
                    user.edit(displayName = "")
                }
            }

            it("인자가 null 이어도 호출이 일어났으면 updatedAt 이 갱신된다") {
                val user: User = basicUser()

                user.edit()

                user.updatedAt shouldNotBe DUMMY_INSTANT
            }
        }
    })
