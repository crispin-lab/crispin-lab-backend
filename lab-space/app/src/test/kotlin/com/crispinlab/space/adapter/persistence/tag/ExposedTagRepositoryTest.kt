package com.crispinlab.space.adapter.persistence.tag

import com.crispinlab.common.exception.ConflictException
import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.persistence.PostgresTestContext
import com.crispinlab.space.adapter.persistence.page.ExposedPageRepository
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.tag.TagErrorCode
import com.crispinlab.space.domain.tag.TagId
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.space.testsupport.Fixtures.basicPageTag
import com.crispinlab.space.testsupport.Fixtures.basicTag
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.Instant
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExposedTagRepositoryTest :
    DescribeSpec({
        val database = PostgresTestContext.database
        val repository = ExposedTagRepository()
        val pageRepository = ExposedPageRepository()

        fun ensurePages(vararg ids: Long) {
            ids.forEach { id ->
                pageRepository.save(basicPage(id = PageId(id)))
            }
        }

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

            it("동일 (space_id, name) 으로 두 번째 save 시 ConflictException 으로 변환된다") {
                transaction(database) {
                    repository.save(
                        basicTag(id = TagId(3L), spaceId = SpaceId(20L), name = "kotlin")
                    )
                }

                val thrown =
                    shouldThrow<ConflictException> {
                        transaction(database) {
                            repository.save(
                                basicTag(id = TagId(4L), spaceId = SpaceId(20L), name = "kotlin")
                            )
                        }
                    }
                thrown.errorCode shouldBe TagErrorCode.TAG_NAME_DUPLICATED
            }

            it("findBySpaceId 는 해당 스페이스의 태그만 페이지로 반환한다") {
                transaction(database) {
                    repository.save(basicTag(id = TagId(10L), spaceId = SpaceId(100L), name = "a"))
                    repository.save(basicTag(id = TagId(11L), spaceId = SpaceId(100L), name = "b"))
                    repository.save(basicTag(id = TagId(12L), spaceId = SpaceId(200L), name = "c"))
                }

                transaction(database) {
                    val result =
                        repository.findBySpaceId(
                            SpaceId(100L),
                            PageRequest(page = 0, size = 20)
                        )
                    result.items.map { it.id } shouldContainExactlyInAnyOrder
                        listOf(TagId(10L), TagId(11L))
                    result.totalElements shouldBe 2L
                    result.page shouldBe 0
                    result.size shouldBe 20
                }
            }

            it("findBySpaceId 는 createdAt ASC 로 정렬된 페이지를 끊어 반환한다") {
                transaction(database) {
                    repository.save(
                        basicTag(
                            id = TagId(13L),
                            spaceId = SpaceId(150L),
                            name = "first",
                            createdAt = Instant.parse("2026-01-01T00:00:00Z")
                        )
                    )
                    repository.save(
                        basicTag(
                            id = TagId(14L),
                            spaceId = SpaceId(150L),
                            name = "second",
                            createdAt = Instant.parse("2026-01-02T00:00:00Z")
                        )
                    )
                    repository.save(
                        basicTag(
                            id = TagId(15L),
                            spaceId = SpaceId(150L),
                            name = "third",
                            createdAt = Instant.parse("2026-01-03T00:00:00Z")
                        )
                    )
                }

                transaction(database) {
                    val firstPage =
                        repository.findBySpaceId(
                            SpaceId(150L),
                            PageRequest(page = 0, size = 2)
                        )
                    firstPage.items.map { it.id } shouldBe listOf(TagId(13L), TagId(14L))
                    firstPage.totalElements shouldBe 3L
                    firstPage.hasNext shouldBe true

                    val secondPage =
                        repository.findBySpaceId(
                            SpaceId(150L),
                            PageRequest(page = 1, size = 2)
                        )
                    secondPage.items.map { it.id } shouldBe listOf(TagId(15L))
                    secondPage.hasNext shouldBe false
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
                    ensurePages(1000L, 1001L)
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

            it("page 가 soft delete 되면 row 가 보존되므로 page_tags 매핑도 그대로 유지된다") {
                transaction(database) {
                    ensurePages(1500L)
                    repository.save(basicTag(id = TagId(35L)))
                    repository.attach(basicPageTag(pageId = PageId(1500L), tagId = TagId(35L)))
                }

                transaction(database) {
                    pageRepository.delete(PageId(1500L))
                }

                transaction(database) {
                    repository.findPageIdsByTagId(TagId(35L)) shouldContainExactlyInAnyOrder
                        listOf(PageId(1500L))
                    repository.findBy(TagId(35L)).shouldNotBeNull()
                }
            }

            it("attach 는 같은 (pageId, tagId) 로 다시 호출해도 멱등이다") {
                transaction(database) {
                    ensurePages(2000L)
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
                    ensurePages(3000L, 3001L)
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

            it("findTagsByPageId 는 page 에 매핑된 모든 tag 를 페이지로 반환한다") {
                transaction(database) {
                    ensurePages(4000L, 4001L)
                    repository.save(basicTag(id = TagId(60L), name = "a"))
                    repository.save(basicTag(id = TagId(61L), name = "b"))
                    repository.save(basicTag(id = TagId(62L), name = "c"))
                    repository.attach(basicPageTag(pageId = PageId(4000L), tagId = TagId(60L)))
                    repository.attach(basicPageTag(pageId = PageId(4000L), tagId = TagId(61L)))
                    repository.attach(basicPageTag(pageId = PageId(4001L), tagId = TagId(62L)))
                }

                transaction(database) {
                    val result =
                        repository.findTagsByPageId(
                            PageId(4000L),
                            PageRequest(page = 0, size = 20)
                        )
                    result.items.map { it.id } shouldContainExactlyInAnyOrder
                        listOf(TagId(60L), TagId(61L))
                    result.totalElements shouldBe 2L
                }
            }

            it("findTagsByPageId 는 createdAt ASC 로 정렬된 페이지를 끊어 반환한다") {
                transaction(database) {
                    ensurePages(4500L)
                    repository.save(
                        basicTag(
                            id = TagId(70L),
                            name = "first",
                            createdAt = Instant.parse("2026-02-01T00:00:00Z")
                        )
                    )
                    repository.save(
                        basicTag(
                            id = TagId(71L),
                            name = "second",
                            createdAt = Instant.parse("2026-02-02T00:00:00Z")
                        )
                    )
                    repository.save(
                        basicTag(
                            id = TagId(72L),
                            name = "third",
                            createdAt = Instant.parse("2026-02-03T00:00:00Z")
                        )
                    )
                    repository.attach(basicPageTag(pageId = PageId(4500L), tagId = TagId(70L)))
                    repository.attach(basicPageTag(pageId = PageId(4500L), tagId = TagId(71L)))
                    repository.attach(basicPageTag(pageId = PageId(4500L), tagId = TagId(72L)))
                }

                transaction(database) {
                    val firstPage =
                        repository.findTagsByPageId(
                            PageId(4500L),
                            PageRequest(page = 0, size = 2)
                        )
                    firstPage.items.map { it.id } shouldBe listOf(TagId(70L), TagId(71L))
                    firstPage.totalElements shouldBe 3L
                    firstPage.hasNext shouldBe true

                    val secondPage =
                        repository.findTagsByPageId(
                            PageId(4500L),
                            PageRequest(page = 1, size = 2)
                        )
                    secondPage.items.map { it.id } shouldBe listOf(TagId(72L))
                    secondPage.hasNext shouldBe false
                }
            }

            it("findTagsByPageId 는 매핑이 없으면 빈 페이지를 반환한다") {
                transaction(database) {
                    val result =
                        repository.findTagsByPageId(
                            PageId(8888L),
                            PageRequest(page = 0, size = 20)
                        )
                    result.items.shouldBeEmpty()
                    result.totalElements shouldBe 0L
                }
            }

            it("findTagsByPageId 는 soft delete 된 page 의 tag 매핑을 노출하지 않지만 page_tags row 는 보존한다") {
                transaction(database) {
                    ensurePages(5000L)
                    repository.save(basicTag(id = TagId(80L), name = "kotlin"))
                    repository.attach(basicPageTag(pageId = PageId(5000L), tagId = TagId(80L)))
                }

                transaction(database) {
                    pageRepository.delete(PageId(5000L))
                }

                transaction(database) {
                    val result =
                        repository.findTagsByPageId(
                            PageId(5000L),
                            PageRequest(page = 0, size = 20)
                        )
                    result.items.shouldBeEmpty()
                    result.totalElements shouldBe 0L
                    repository.findPageIdsByTagId(TagId(80L)) shouldContainExactlyInAnyOrder
                        listOf(PageId(5000L))
                }
            }

            it("findIdsByName 은 cross-space 로 같은 이름의 모든 tagId 를 모은다") {
                transaction(database) {
                    repository.save(
                        basicTag(id = TagId(91L), spaceId = SpaceId(10L), name = "kotlin")
                    )
                    repository.save(
                        basicTag(id = TagId(92L), spaceId = SpaceId(20L), name = "kotlin")
                    )
                    repository.save(
                        basicTag(id = TagId(93L), spaceId = SpaceId(30L), name = "scala")
                    )
                }

                transaction(database) {
                    repository.findIdsByName("kotlin") shouldContainExactlyInAnyOrder
                        listOf(TagId(91L), TagId(92L))
                }
            }

            it("findIdsByName 은 매치되는 tag 가 없으면 빈 리스트를 반환한다") {
                transaction(database) {
                    repository.save(basicTag(id = TagId(94L), name = "kotlin"))
                }

                transaction(database) {
                    repository.findIdsByName("존재하지않음").shouldBeEmpty()
                }
            }
        }
    })
