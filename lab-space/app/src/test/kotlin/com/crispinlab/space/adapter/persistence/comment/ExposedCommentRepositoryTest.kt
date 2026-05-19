package com.crispinlab.space.adapter.persistence.comment

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.persistence.PostgresTestContext
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.user.UserId
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.Fixtures.basicComment
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExposedCommentRepositoryTest :
    DescribeSpec({
        val database = PostgresTestContext.database
        val repository = ExposedCommentRepository()

        afterEach {
            PostgresTestContext.truncateAll()
        }

        describe("ExposedCommentRepository") {
            it("save 후 별도 트랜잭션의 findBy 로 동일 entity 가 복원된다") {
                transaction(database) {
                    repository.save(
                        basicComment(
                            id = CommentId(1L),
                            pageId = PageId(10L),
                            body = "안녕하세요"
                        )
                    )
                }

                transaction(database) {
                    val found = repository.findBy(CommentId(1L))

                    found.shouldNotBeNull()
                    found.id shouldBe CommentId(1L)
                    found.pageId shouldBe PageId(10L)
                    found.body shouldBe "안녕하세요"
                    found.deletedAt.shouldBeNull()
                    found.isDeleted shouldBe false
                }
            }

            it("같은 ID 로 다시 save 하면 body·updatedAt 만 갱신되고 pageId·authorId·createdAt 은 보존된다") {
                transaction(database) {
                    repository.save(
                        basicComment(
                            id = CommentId(2L),
                            pageId = PageId(50L),
                            authorId = UserId(500L),
                            body = "이전"
                        )
                    )
                }

                val originalUpdatedAt =
                    transaction(database) {
                        val original = repository.findBy(CommentId(2L)).shouldNotBeNull()
                        val capturedUpdatedAt = original.updatedAt
                        original.edit(body = "수정됨")
                        repository.save(original)
                        capturedUpdatedAt
                    }

                transaction(database) {
                    val updated = repository.findBy(CommentId(2L)).shouldNotBeNull()
                    updated.body shouldBe "수정됨"
                    updated.pageId shouldBe PageId(50L)
                    updated.authorId shouldBe UserId(500L)
                    updated.createdAt shouldBe DUMMY_INSTANT
                    updated.updatedAt shouldNotBe originalUpdatedAt
                }
            }

            it("findByPageId 는 해당 페이지의 댓글만 paging 으로 반환한다") {
                transaction(database) {
                    repository.save(basicComment(id = CommentId(10L), pageId = PageId(100L)))
                    repository.save(basicComment(id = CommentId(11L), pageId = PageId(100L)))
                    repository.save(basicComment(id = CommentId(12L), pageId = PageId(100L)))
                    repository.save(basicComment(id = CommentId(20L), pageId = PageId(200L)))
                }

                transaction(database) {
                    val result =
                        repository.findByPageId(
                            pageId = PageId(100L),
                            pageRequest = PageRequest(page = 0, size = 2)
                        )

                    result.items shouldHaveSize 2
                    result.totalElements shouldBe 3L
                    result.totalPages shouldBe 2
                    result.hasNext shouldBe true
                    result.items.all { it.pageId == PageId(100L) } shouldBe true
                }
            }

            it("findByPageId 는 createdAt ASC, id ASC tiebreaker 순으로 정렬해 반환한다") {
                transaction(database) {
                    repository.save(
                        basicComment(id = CommentId(43L), pageId = PageId(300L))
                    )
                    repository.save(
                        basicComment(id = CommentId(41L), pageId = PageId(300L))
                    )
                    repository.save(
                        basicComment(id = CommentId(42L), pageId = PageId(300L))
                    )
                }

                transaction(database) {
                    val result =
                        repository.findByPageId(
                            pageId = PageId(300L),
                            pageRequest = PageRequest(page = 0, size = 20)
                        )

                    result.items.map { it.id } shouldBe
                        listOf(CommentId(41L), CommentId(42L), CommentId(43L))
                }
            }

            it("findByPageId 는 soft-deleted 댓글을 자동으로 제외한다") {
                transaction(database) {
                    repository.save(basicComment(id = CommentId(60L), pageId = PageId(400L)))
                    repository.save(basicComment(id = CommentId(61L), pageId = PageId(400L)))
                    repository.delete(CommentId(61L))
                }

                transaction(database) {
                    val result =
                        repository.findByPageId(
                            pageId = PageId(400L),
                            pageRequest = PageRequest(page = 0, size = 20)
                        )

                    result.items shouldHaveSize 1
                    result.totalElements shouldBe 1L
                    result.items.all { it.isDeleted.not() } shouldBe true
                    result.items.first().id shouldBe CommentId(60L)
                }
            }

            it("findByPageId 는 해당 페이지에 댓글이 없으면 빈 페이지를 반환한다") {
                transaction(database) {
                    val result =
                        repository.findByPageId(
                            pageId = PageId(999L),
                            pageRequest = PageRequest(page = 0, size = 20)
                        )

                    result.items shouldHaveSize 0
                    result.totalElements shouldBe 0L
                    result.isEmpty shouldBe true
                }
            }

            it("repository.delete 는 soft delete 로 동작 — row 는 보존되고 findBy 는 null 을 반환한다") {
                transaction(database) {
                    repository.save(basicComment(id = CommentId(30L)))
                }

                transaction(database) {
                    repository.delete(CommentId(30L))
                }

                transaction(database) {
                    repository.findBy(CommentId(30L)).shouldBeNull()
                    val row =
                        Comments
                            .selectAll()
                            .where { Comments.id eq 30L }
                            .firstOrNull()
                            .shouldNotBeNull()
                    row[Comments.deletedAt].shouldNotBeNull()
                }
            }

            it("save 가 soft delete 된 row 의 deleted_at 을 덮지 않는다") {
                val originalDeletedAt =
                    transaction(database) {
                        repository.save(basicComment(id = CommentId(100L)))
                        repository.delete(CommentId(100L))
                        Comments
                            .selectAll()
                            .where { Comments.id eq 100L }
                            .first()[Comments.deletedAt]
                    }.shouldNotBeNull()

                transaction(database) {
                    repository.save(
                        basicComment(id = CommentId(100L), body = "복구 시도", deletedAt = null)
                    )
                }

                transaction(database) {
                    val row =
                        Comments
                            .selectAll()
                            .where { Comments.id eq 100L }
                            .first()
                    row[Comments.deletedAt] shouldBe originalDeletedAt
                    repository.findBy(CommentId(100L)).shouldBeNull()
                }
            }

            it("이미 soft delete 된 row 에 delete 를 다시 호출해도 deletedAt 이 갱신되지 않는다") {
                val firstDeletedAt =
                    transaction(database) {
                        repository.save(basicComment(id = CommentId(31L)))
                        repository.delete(CommentId(31L))
                        Comments
                            .selectAll()
                            .where { Comments.id eq 31L }
                            .first()[Comments.deletedAt]
                    }.shouldNotBeNull()

                transaction(database) {
                    repository.delete(CommentId(31L))
                }

                transaction(database) {
                    val row =
                        Comments
                            .selectAll()
                            .where { Comments.id eq 31L }
                            .first()
                    row[Comments.deletedAt] shouldBe firstDeletedAt
                }
            }
        }
    })
