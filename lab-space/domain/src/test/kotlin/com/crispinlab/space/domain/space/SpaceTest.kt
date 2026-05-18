package com.crispinlab.space.domain.space

import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.Fixtures.basicSpace
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class SpaceTest :
    DescribeSpec({
        describe("생성") {
            it("정상 생성된다") {
                val space: Space = basicSpace()

                space.name shouldBe "자유게시판"
                space.updatedAt shouldBe DUMMY_INSTANT
            }

            it("이름이 비어 있으면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicSpace(name = "")
                }
            }

            it("이름이 ${Space.MAX_NAME_LENGTH}자를 넘으면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicSpace(name = "a".repeat(Space.MAX_NAME_LENGTH + 1))
                }
            }
        }

        describe("이름·설명 수정") {
            it("모두 변경하면 updatedAt 도 새 값으로 바뀐다") {
                val space: Space = basicSpace()

                space.edit(name = "공지사항", description = "공식 공지")

                space.name shouldBe "공지사항"
                space.description shouldBe "공식 공지"
                space.updatedAt shouldNotBe DUMMY_INSTANT
            }

            it("이름만 넘기면 설명은 그대로 유지된다") {
                val space: Space = basicSpace()

                space.edit(name = "공지사항")

                space.name shouldBe "공지사항"
                space.description shouldBe "기본 설명"
                space.updatedAt shouldNotBe DUMMY_INSTANT
            }

            it("새 이름이 비어 있으면 실패한다") {
                val space: Space = basicSpace()

                shouldThrow<IllegalArgumentException> {
                    space.edit(name = "")
                }
            }

            it("변경 인자가 모두 null 이어도 호출이 일어났으면 updatedAt 이 갱신된다") {
                val space: Space = basicSpace()

                space.edit()

                space.updatedAt shouldNotBe DUMMY_INSTANT
            }
        }

        describe("soft delete") {
            it("delete() 호출 시 deletedAt 과 updatedAt 이 같은 시점으로 갱신된다") {
                val space: Space = basicSpace()

                space.delete()

                space.isDeleted shouldBe true
                val occurredAt = space.deletedAt.shouldNotBeNull()
                space.updatedAt shouldBe occurredAt
            }

            it("이미 삭제된 스페이스에 delete() 를 다시 호출하면 실패한다") {
                val space: Space = basicSpace().also { it.delete() }

                shouldThrow<IllegalStateException> {
                    space.delete()
                }
            }

            it("삭제된 스페이스는 edit() 가 실패한다") {
                val space: Space = basicSpace().also { it.delete() }

                shouldThrow<IllegalStateException> {
                    space.edit(name = "변경")
                }
            }
        }
    })
