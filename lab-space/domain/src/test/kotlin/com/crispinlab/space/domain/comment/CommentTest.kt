package com.crispinlab.space.domain.comment

import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.Fixtures.basicComment
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class CommentTest :
    DescribeSpec({
        describe("생성") {
            it("정상 생성된다") {
                val comment: Comment = basicComment()

                comment.content.raw shouldBe "댓글"
                comment.isDeleted shouldBe false
            }
        }

        describe("본문 수정") {
            it("content 와 updatedAt 이 갱신된다") {
                val comment: Comment = basicComment()

                comment.edit(content = CommentContent("수정된 댓글"))

                comment.content.raw shouldBe "수정된 댓글"
                comment.updatedAt shouldNotBe DUMMY_INSTANT
            }

            it("삭제된 댓글은 수정할 수 없다") {
                val comment: Comment = basicComment(deletedAt = DUMMY_INSTANT)

                shouldThrow<IllegalStateException> {
                    comment.edit(content = CommentContent("수정 시도"))
                }
            }
        }
    })
