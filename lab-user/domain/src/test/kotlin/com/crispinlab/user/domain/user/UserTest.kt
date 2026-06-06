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

                user.handle shouldBe Handle("test_user")
                user.role shouldBe SystemRole.USER
                user.updatedAt shouldBe DUMMY_INSTANT
                user.isDeleted shouldBe false
            }
        }

        describe("사용자 이름 변경") {
            it("정상 변경 시 handle 과 updatedAt 이 갱신된다") {
                val user: User = basicUser()

                user.changeHandle(Handle("new_handle"))

                user.handle shouldBe Handle("new_handle")
                user.updatedAt shouldNotBe DUMMY_INSTANT
            }

            it("삭제된 사용자는 사용자 이름을 변경할 수 없다") {
                val user: User = basicUser(deletedAt = DUMMY_INSTANT)

                shouldThrow<IllegalStateException> {
                    user.changeHandle(Handle("new_handle"))
                }
            }
        }

        describe("역할 변경") {
            it("정상 변경 시 role 과 updatedAt 이 갱신된다") {
                val user: User = basicUser()

                user.promoteTo(SystemRole.ADMIN)

                user.role shouldBe SystemRole.ADMIN
                user.updatedAt shouldNotBe DUMMY_INSTANT
            }

            it("삭제된 사용자는 역할을 변경할 수 없다") {
                val user: User = basicUser(deletedAt = DUMMY_INSTANT)

                shouldThrow<IllegalStateException> {
                    user.promoteTo(SystemRole.ADMIN)
                }
            }
        }

        describe("SoftDeletable") {
            it("deletedAt 이 null 이면 isDeleted 가 false") {
                basicUser(deletedAt = null).isDeleted shouldBe false
            }

            it("deletedAt 이 있으면 isDeleted 가 true") {
                basicUser(deletedAt = DUMMY_INSTANT).isDeleted shouldBe true
            }
        }
    })
