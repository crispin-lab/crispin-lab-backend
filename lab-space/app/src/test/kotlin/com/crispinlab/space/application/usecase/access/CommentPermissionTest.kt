package com.crispinlab.space.application.usecase.access

import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.user.domain.user.UserId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CommentPermissionTest :
    DescribeSpec({
        val authorId = UserId(100L)

        describe("canEditCommentOf") {
            it("ADMIN 은 타인 댓글이어도 true") {
                val viewer = Viewer.Member(userId = UserId(999L), isAdmin = true)

                viewer.canEditCommentOf(authorId) shouldBe true
            }

            it("본인 댓글이면 true (스페이스 멤버십 무관)") {
                val viewer = Viewer.Member(userId = authorId, isAdmin = false)

                viewer.canEditCommentOf(authorId) shouldBe true
            }

            it("타인 댓글이면 false") {
                val viewer = Viewer.Member(userId = UserId(200L), isAdmin = false)

                viewer.canEditCommentOf(authorId) shouldBe false
            }

            it("Anonymous 는 false") {
                Viewer.Anonymous.canEditCommentOf(authorId) shouldBe false
            }
        }
    })
