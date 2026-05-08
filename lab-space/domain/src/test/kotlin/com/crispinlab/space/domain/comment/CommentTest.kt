package com.crispinlab.space.domain.comment

import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.Fixtures.basicComment
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CommentTest :
    DescribeSpec({
        describe("init") {
            it("정상 생성") {
                val comment: Comment = basicComment()

                comment.body shouldBe "댓글"
                comment.isDeleted shouldBe false
            }

            it("body 가 비어 있으면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicComment(body = "")
                }
            }
        }

        describe("edit") {
            it("body 와 updatedAt 이 갱신된다") {
                val comment: Comment = basicComment()
                val occurredAt = DUMMY_INSTANT.plusSeconds(60)

                comment.edit(body = "수정된 댓글", occurredAt = occurredAt)

                comment.body shouldBe "수정된 댓글"
                comment.updatedAt shouldBe occurredAt
            }

            it("삭제된 댓글은 수정할 수 없다") {
                val comment: Comment =
                    basicComment().also { it.delete(DUMMY_INSTANT.plusSeconds(60)) }

                shouldThrow<IllegalStateException> {
                    comment.edit(body = "수정 시도", occurredAt = DUMMY_INSTANT.plusSeconds(120))
                }
            }

            it("새 body 가 비어 있으면 실패한다") {
                val comment: Comment = basicComment()

                shouldThrow<IllegalArgumentException> {
                    comment.edit(body = "", occurredAt = DUMMY_INSTANT.plusSeconds(60))
                }
            }
        }

        describe("delete") {
            it("deletedAt 과 updatedAt 이 모두 갱신된다") {
                val comment: Comment = basicComment()
                val occurredAt = DUMMY_INSTANT.plusSeconds(60)

                comment.delete(occurredAt = occurredAt)

                comment.deletedAt shouldBe occurredAt
                comment.updatedAt shouldBe occurredAt
                comment.isDeleted shouldBe true
            }

            it("이미 삭제된 댓글을 다시 삭제할 수 없다") {
                val comment: Comment =
                    basicComment().also { it.delete(DUMMY_INSTANT.plusSeconds(60)) }

                shouldThrow<IllegalStateException> {
                    comment.delete(occurredAt = DUMMY_INSTANT.plusSeconds(120))
                }
            }
        }
    })
