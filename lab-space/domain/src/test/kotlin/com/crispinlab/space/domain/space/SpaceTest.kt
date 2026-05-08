package com.crispinlab.space.domain.space

import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.Fixtures.basicSpace
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

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

        describe("update") {
            it("이름과 설명, updatedAt 이 모두 갱신된다") {
                val space: Space = basicSpace()
                val occurredAt = DUMMY_INSTANT.plusSeconds(60)

                space.update(name = "공지사항", description = "공식 공지", occurredAt = occurredAt)

                space.name shouldBe "공지사항"
                space.description shouldBe "공식 공지"
                space.updatedAt shouldBe occurredAt
            }

            it("이름만 넘기면 설명은 그대로 두고 updatedAt 만 갱신된다") {
                val space: Space = basicSpace()
                val occurredAt = DUMMY_INSTANT.plusSeconds(60)

                space.update(name = "공지사항", occurredAt = occurredAt)

                space.name shouldBe "공지사항"
                space.description shouldBe "기본 설명"
                space.updatedAt shouldBe occurredAt
            }

            it("새 이름이 비어 있으면 실패한다") {
                val space: Space = basicSpace()

                shouldThrow<IllegalArgumentException> {
                    space.update(name = "", occurredAt = DUMMY_INSTANT.plusSeconds(60))
                }
            }

            it("변경 인자가 모두 null 이면 updatedAt 도 그대로 둔다") {
                val space: Space = basicSpace()

                space.update(occurredAt = DUMMY_INSTANT.plusSeconds(60))

                space.updatedAt shouldBe DUMMY_INSTANT
            }
        }
    })
