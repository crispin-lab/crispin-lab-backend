package com.crispinlab.space.adapter.persistence.comment

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.user.UserId
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.Fixtures.basicComment
import com.crispinlab.space.testsupport.PostgresTestContext
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
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

                transaction(database) {
                    val original = repository.findBy(CommentId(2L)).shouldNotBeNull()
                    original.edit(body = "수정됨")
                    repository.save(original)
                }

                transaction(database) {
                    val updated = repository.findBy(CommentId(2L)).shouldNotBeNull()
                    updated.body shouldBe "수정됨"
                    updated.pageId shouldBe PageId(50L)
                    updated.authorId shouldBe UserId(500L)
                    updated.createdAt shouldBe DUMMY_INSTANT
                }
            }

            it("soft delete 후 deletedAt 이 보존된다") {
                transaction(database) {
                    repository.save(basicComment(id = CommentId(3L)))
                }

                transaction(database) {
                    val comment = repository.findBy(CommentId(3L)).shouldNotBeNull()
                    comment.delete()
                    repository.save(comment)
                }

                transaction(database) {
                    val found = repository.findBy(CommentId(3L)).shouldNotBeNull()
                    found.isDeleted shouldBe true
                    found.deletedAt.shouldNotBeNull()
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

            it("findByPageId 는 soft-deleted 댓글도 결과에 포함한다 (client 가 isDeleted 로 판단)") {
                transaction(database) {
                    repository.save(basicComment(id = CommentId(60L), pageId = PageId(400L)))
                    val toDelete = basicComment(id = CommentId(61L), pageId = PageId(400L))
                    toDelete.delete()
                    repository.save(toDelete)
                }

                transaction(database) {
                    val result =
                        repository.findByPageId(
                            pageId = PageId(400L),
                            pageRequest = PageRequest(page = 0, size = 20)
                        )

                    result.items shouldHaveSize 2
                    result.totalElements shouldBe 2L
                    result.items.any { it.isDeleted } shouldBe true
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

            it("delete 후에는 findBy 가 null 을 반환한다 (물리 삭제)") {
                transaction(database) {
                    repository.save(basicComment(id = CommentId(30L)))
                }

                transaction(database) {
                    repository.delete(CommentId(30L))
                }

                transaction(database) {
                    repository.findBy(CommentId(30L)).shouldBeNull()
                }
            }
        }
    })
