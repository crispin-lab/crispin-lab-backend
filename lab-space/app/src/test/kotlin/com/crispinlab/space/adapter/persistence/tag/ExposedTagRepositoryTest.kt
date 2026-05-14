package com.crispinlab.space.adapter.persistence.tag

import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.tag.TagId
import com.crispinlab.space.testsupport.Fixtures.basicPageTag
import com.crispinlab.space.testsupport.Fixtures.basicTag
import com.crispinlab.space.testsupport.PostgresTestContext
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExposedTagRepositoryTest :
    DescribeSpec({
        val database = PostgresTestContext.database
        val repository = ExposedTagRepository()

        afterEach {
            PostgresTestContext.truncateAll()
        }

        describe("ExposedTagRepository") {
            it("save 후 별도 트랜잭션의 findBy 로 동일 entity 가 복원된다") {
                transaction(database) {
                    repository.save(
                        basicTag(
                            id = TagId(1L),
                            spaceId = SpaceId(10L),
                            name = "kotlin"
                        )
                    )
                }

                transaction(database) {
                    val found = repository.findBy(TagId(1L))

                    found.shouldNotBeNull()
                    found.id shouldBe TagId(1L)
                    found.spaceId shouldBe SpaceId(10L)
                    found.name shouldBe "kotlin"
                }
            }

            it("같은 ID 로 다시 save 하면 name 만 갱신된다") {
                transaction(database) {
                    repository.save(basicTag(id = TagId(2L), name = "scala"))
                }

                transaction(database) {
                    val tag = repository.findBy(TagId(2L)).shouldNotBeNull()
                    tag.rename("kotlin")
                    repository.save(tag)
                }

                transaction(database) {
                    val updated = repository.findBy(TagId(2L)).shouldNotBeNull()
                    updated.name shouldBe "kotlin"
                }
            }

            it("(space_id, name) UNIQUE 제약 위반 시 ExposedSQLException 이 발생한다") {
                transaction(database) {
                    repository.save(
                        basicTag(id = TagId(3L), spaceId = SpaceId(20L), name = "kotlin")
                    )
                }

                shouldThrow<ExposedSQLException> {
                    transaction(database) {
                        repository.save(
                            basicTag(id = TagId(4L), spaceId = SpaceId(20L), name = "kotlin")
                        )
                    }
                }
            }

            it("findBySpaceId 는 해당 스페이스의 태그만 반환한다") {
                transaction(database) {
                    repository.save(basicTag(id = TagId(10L), spaceId = SpaceId(100L), name = "a"))
                    repository.save(basicTag(id = TagId(11L), spaceId = SpaceId(100L), name = "b"))
                    repository.save(basicTag(id = TagId(12L), spaceId = SpaceId(200L), name = "c"))
                }

                transaction(database) {
                    val tags = repository.findBySpaceId(SpaceId(100L))
                    tags.map { it.id } shouldContainExactlyInAnyOrder listOf(TagId(10L), TagId(11L))
                }
            }

            it("existsByNameAndSpaceId 는 동일 space 내 같은 name 존재 여부를 반환한다") {
                transaction(database) {
                    repository.save(
                        basicTag(id = TagId(20L), spaceId = SpaceId(300L), name = "kotlin")
                    )
                }

                transaction(database) {
                    repository.existsByNameAndSpaceId(SpaceId(300L), "kotlin") shouldBe true
                    repository.existsByNameAndSpaceId(SpaceId(300L), "scala") shouldBe false
                    repository.existsByNameAndSpaceId(SpaceId(999L), "kotlin") shouldBe false
                }
            }

            it("delete 는 tags 와 연관된 page_tags 매핑을 함께 삭제한다") {
                transaction(database) {
                    repository.save(basicTag(id = TagId(30L), spaceId = SpaceId(400L)))
                    repository.attach(basicPageTag(pageId = PageId(1000L), tagId = TagId(30L)))
                    repository.attach(basicPageTag(pageId = PageId(1001L), tagId = TagId(30L)))
                }

                transaction(database) {
                    repository.delete(TagId(30L))
                }

                transaction(database) {
                    repository.findBy(TagId(30L)).shouldBeNull()
                    repository.findPageIdsByTagId(TagId(30L)).shouldBeEmpty()
                }
            }

            it("attach 는 같은 (pageId, tagId) 로 다시 호출해도 멱등이다") {
                transaction(database) {
                    repository.save(basicTag(id = TagId(40L)))
                }

                transaction(database) {
                    repository.attach(basicPageTag(pageId = PageId(2000L), tagId = TagId(40L)))
                    repository.attach(basicPageTag(pageId = PageId(2000L), tagId = TagId(40L)))
                }

                transaction(database) {
                    repository.findPageIdsByTagId(TagId(40L)) shouldHaveSize 1
                }
            }

            it("detach 후에는 해당 매핑만 사라지고 다른 매핑은 보존된다") {
                transaction(database) {
                    repository.save(basicTag(id = TagId(50L)))
                    repository.attach(basicPageTag(pageId = PageId(3000L), tagId = TagId(50L)))
                    repository.attach(basicPageTag(pageId = PageId(3001L), tagId = TagId(50L)))
                }

                transaction(database) {
                    repository.detach(PageId(3000L), TagId(50L))
                }

                transaction(database) {
                    repository.findPageIdsByTagId(TagId(50L)) shouldContainExactlyInAnyOrder
                        listOf(PageId(3001L))
                }
            }

            it("detach 는 매핑이 없어도 예외 없이 성공한다 (멱등)") {
                transaction(database) {
                    repository.detach(PageId(9999L), TagId(9999L))
                }
            }

            it("findTagsByPageId 는 page 에 매핑된 모든 tag 를 반환한다") {
                transaction(database) {
                    repository.save(basicTag(id = TagId(60L), name = "a"))
                    repository.save(basicTag(id = TagId(61L), name = "b"))
                    repository.save(basicTag(id = TagId(62L), name = "c"))
                    repository.attach(basicPageTag(pageId = PageId(4000L), tagId = TagId(60L)))
                    repository.attach(basicPageTag(pageId = PageId(4000L), tagId = TagId(61L)))
                    repository.attach(basicPageTag(pageId = PageId(4001L), tagId = TagId(62L)))
                }

                transaction(database) {
                    repository
                        .findTagsByPageId(
                            PageId(4000L)
                        ).map { it.id } shouldContainExactlyInAnyOrder
                        listOf(TagId(60L), TagId(61L))
                }
            }

            it("findTagsByPageId 는 매핑이 없으면 빈 리스트를 반환한다") {
                transaction(database) {
                    repository.findTagsByPageId(PageId(8888L)).shouldBeEmpty()
                }
            }
        }
    })
