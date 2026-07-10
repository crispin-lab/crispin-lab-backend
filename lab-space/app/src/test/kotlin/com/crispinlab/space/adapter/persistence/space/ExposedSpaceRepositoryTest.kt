package com.crispinlab.space.adapter.persistence.space

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.persistence.PostgresTestContext
import com.crispinlab.space.adapter.persistence.page.ExposedPageRepository
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository.SortDirection
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository.SortOption
import com.crispinlab.space.application.port.outgoing.space.SpaceVisibilityScope
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.space.testsupport.Fixtures.basicSpace
import com.crispinlab.user.domain.user.UserId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExposedSpaceRepositoryTest :
    DescribeSpec({
        val database = PostgresTestContext.database
        val repository = ExposedSpaceRepository()
        val pageRepository = ExposedPageRepository()
        val privileged: SpaceVisibilityScope = SpaceVisibilityScope.Privileged

        afterEach {
            PostgresTestContext.truncateAll()
        }

        describe("ExposedSpaceRepository") {
            it("save 한 뒤 별도 transaction 의 findBy 로 동일 entity 가 복원된다") {
                transaction(database) {
                    repository.save(
                        basicSpace(id = SpaceId(1L), name = "팀 위키", description = "공유 공간")
                    )
                }

                transaction(database) {
                    val found = repository.findBy(SpaceId(1L))

                    found.shouldNotBeNull()
                    found.id shouldBe SpaceId(1L)
                    found.name shouldBe "팀 위키"
                    found.description shouldBe "공유 공간"
                }
            }

            it("같은 ID 로 다시 save 하면 update 가 일어난다") {
                transaction(database) {
                    repository.save(basicSpace(id = SpaceId(2L), name = "이전"))
                }

                transaction(database) {
                    val existing = repository.findBy(SpaceId(2L))!!
                    existing.edit(name = "새로운")
                    repository.save(existing)
                }

                transaction(database) {
                    repository.findBy(SpaceId(2L))?.name shouldBe "새로운"
                }
            }

            it("repository.delete 는 soft delete 로 동작 — row 는 보존되고 findBy 는 null 을 반환한다") {
                transaction(database) {
                    repository.save(basicSpace(id = SpaceId(5L)))
                }

                transaction(database) {
                    repository.delete(SpaceId(5L))
                }

                transaction(database) {
                    repository.findBy(SpaceId(5L)).shouldBeNull()
                    val row =
                        Spaces
                            .selectAll()
                            .where { Spaces.id eq 5L }
                            .firstOrNull()
                            .shouldNotBeNull()
                    row[Spaces.deletedAt].shouldNotBeNull()
                }
            }

            it("findPage 는 soft deleted 스페이스를 자동 제외한다") {
                transaction(database) {
                    repository.save(basicSpace(id = SpaceId(40L), createdAt = DUMMY_INSTANT))
                    repository.save(
                        basicSpace(id = SpaceId(41L), createdAt = DUMMY_INSTANT.plusSeconds(60))
                    )
                }

                transaction(database) {
                    repository.delete(SpaceId(40L))
                }

                transaction(database) {
                    val result =
                        repository.findPage(
                            PageRequest(page = 0, size = 10),
                            privileged
                        )
                    result.totalElements shouldBe 1L
                    result.items.map { it.spaceId } shouldBe listOf(SpaceId(41L))
                }
            }

            it(
                "findPage 는 default sort (LAST_ACTIVITY_AT DESC) 로 페이지 없을 때 space.updatedAt DESC 순으로 반환"
            ) {
                transaction(database) {
                    repository.save(
                        basicSpace(id = SpaceId(11L), name = "오래된", createdAt = DUMMY_INSTANT)
                    )
                    repository.save(
                        basicSpace(
                            id = SpaceId(12L),
                            name = "중간",
                            createdAt = DUMMY_INSTANT.plusSeconds(60)
                        )
                    )
                    repository.save(
                        basicSpace(
                            id = SpaceId(13L),
                            name = "최근",
                            createdAt = DUMMY_INSTANT.plusSeconds(120)
                        )
                    )
                }

                transaction(database) {
                    val result =
                        repository.findPage(
                            PageRequest(page = 0, size = 10),
                            privileged
                        )

                    result.totalElements shouldBe 3L
                    result.items.map { it.spaceId } shouldBe
                        listOf(SpaceId(13L), SpaceId(12L), SpaceId(11L))
                    result.items.map { it.lastActivityAt } shouldBe
                        listOf(
                            DUMMY_INSTANT.plusSeconds(120),
                            DUMMY_INSTANT.plusSeconds(60),
                            DUMMY_INSTANT
                        )
                }
            }

            it("findPage 는 offset/limit 을 반영한다") {
                transaction(database) {
                    (1..5).forEach { index ->
                        repository.save(
                            basicSpace(
                                id = SpaceId(20L + index),
                                name = "스페이스 $index",
                                createdAt = DUMMY_INSTANT.plusSeconds(index.toLong())
                            )
                        )
                    }
                }

                transaction(database) {
                    val secondPage =
                        repository.findPage(
                            PageRequest(page = 1, size = 2),
                            privileged
                        )

                    secondPage.page shouldBe 1
                    secondPage.size shouldBe 2
                    secondPage.totalElements shouldBe 5L
                    secondPage.items.map { it.spaceId } shouldBe
                        listOf(SpaceId(23L), SpaceId(22L))
                }
            }

            it("findPage 는 데이터가 없으면 빈 페이지를 돌려준다") {
                transaction(database) {
                    val result =
                        repository.findPage(
                            PageRequest(page = 0, size = 10),
                            privileged
                        )

                    result.items shouldBe emptyList()
                    result.totalElements shouldBe 0L
                }
            }

            it("save 가 soft delete 된 row 의 deleted_at 을 덮지 않는다") {
                val originalDeletedAt =
                    transaction(database) {
                        repository.save(basicSpace(id = SpaceId(100L)))
                        repository.delete(SpaceId(100L))
                        Spaces
                            .selectAll()
                            .where { Spaces.id eq 100L }
                            .first()[Spaces.deletedAt]
                    }.shouldNotBeNull()

                transaction(database) {
                    repository.save(
                        basicSpace(id = SpaceId(100L), name = "복구 시도", deletedAt = null)
                    )
                }

                transaction(database) {
                    val row =
                        Spaces
                            .selectAll()
                            .where { Spaces.id eq 100L }
                            .first()
                    row[Spaces.deletedAt] shouldBe originalDeletedAt
                    repository.findBy(SpaceId(100L)).shouldBeNull()
                }
            }

            it("save 는 immutable 컬럼 (createdAt) 을 덮지 않는다") {
                transaction(database) {
                    repository.save(
                        basicSpace(id = SpaceId(110L), name = "원본", createdAt = DUMMY_INSTANT)
                    )
                }

                transaction(database) {
                    repository.save(
                        basicSpace(
                            id = SpaceId(110L),
                            name = "수정 시도",
                            createdAt = DUMMY_INSTANT.plusSeconds(60)
                        )
                    )
                }

                transaction(database) {
                    val found = repository.findBy(SpaceId(110L)).shouldNotBeNull()
                    found.name shouldBe "수정 시도"
                    found.createdAt shouldBe DUMMY_INSTANT
                }
            }

            it("findPage 는 정렬 primary 컬럼이 동일하면 id DESC 로 결정적으로 정렬한다") {
                transaction(database) {
                    repository.save(
                        basicSpace(id = SpaceId(31L), name = "첫번째", createdAt = DUMMY_INSTANT)
                    )
                    repository.save(
                        basicSpace(id = SpaceId(32L), name = "두번째", createdAt = DUMMY_INSTANT)
                    )
                    repository.save(
                        basicSpace(id = SpaceId(33L), name = "세번째", createdAt = DUMMY_INSTANT)
                    )
                }

                transaction(database) {
                    val result =
                        repository.findPage(
                            PageRequest(page = 0, size = 10),
                            privileged
                        )

                    result.items.map { it.spaceId } shouldBe
                        listOf(SpaceId(33L), SpaceId(32L), SpaceId(31L))
                }
            }

            it("Anonymous scope 는 PUBLIC 만 반환한다") {
                transaction(database) {
                    repository.save(
                        basicSpace(
                            id = SpaceId(50L),
                            name = "공개",
                            visibility = SpaceVisibility.PUBLIC,
                            createdAt = DUMMY_INSTANT
                        )
                    )
                    repository.save(
                        basicSpace(
                            id = SpaceId(51L),
                            name = "내부",
                            visibility = SpaceVisibility.INTERNAL,
                            createdAt = DUMMY_INSTANT.plusSeconds(1)
                        )
                    )
                }

                transaction(database) {
                    val result =
                        repository.findPage(
                            PageRequest(page = 0, size = 10),
                            SpaceVisibilityScope.Anonymous
                        )
                    result.items.map { it.spaceId } shouldBe listOf(SpaceId(50L))
                    result.totalElements shouldBe 1L
                }
            }

            it("Authenticated scope 는 PUBLIC + 멤버 INTERNAL 만 반환한다") {
                transaction(database) {
                    repository.save(
                        basicSpace(
                            id = SpaceId(60L),
                            name = "공개",
                            visibility = SpaceVisibility.PUBLIC,
                            createdAt = DUMMY_INSTANT
                        )
                    )
                    repository.save(
                        basicSpace(
                            id = SpaceId(61L),
                            name = "내부 멤버",
                            visibility = SpaceVisibility.INTERNAL,
                            createdAt = DUMMY_INSTANT.plusSeconds(1)
                        )
                    )
                    repository.save(
                        basicSpace(
                            id = SpaceId(62L),
                            name = "내부 비멤버",
                            visibility = SpaceVisibility.INTERNAL,
                            createdAt = DUMMY_INSTANT.plusSeconds(2)
                        )
                    )
                }

                transaction(database) {
                    val result =
                        repository.findPage(
                            PageRequest(page = 0, size = 10),
                            SpaceVisibilityScope.Authenticated(
                                viewerId = UserId(100L),
                                memberOfSpaceIds = setOf(SpaceId(61L))
                            )
                        )
                    result.items.map { it.spaceId } shouldBe listOf(SpaceId(61L), SpaceId(60L))
                    result.totalElements shouldBe 2L
                }
            }

            it("Authenticated scope 의 memberOfSpaceIds 가 비어 있으면 PUBLIC 만 반환한다") {
                transaction(database) {
                    repository.save(
                        basicSpace(
                            id = SpaceId(70L),
                            visibility = SpaceVisibility.PUBLIC,
                            createdAt = DUMMY_INSTANT
                        )
                    )
                    repository.save(
                        basicSpace(
                            id = SpaceId(71L),
                            visibility = SpaceVisibility.INTERNAL,
                            createdAt = DUMMY_INSTANT.plusSeconds(1)
                        )
                    )
                }

                transaction(database) {
                    val result =
                        repository.findPage(
                            PageRequest(page = 0, size = 10),
                            SpaceVisibilityScope.Authenticated(
                                viewerId = UserId(100L),
                                memberOfSpaceIds = emptySet()
                            )
                        )
                    result.items.map { it.spaceId } shouldBe listOf(SpaceId(70L))
                }
            }

            it("keyword 는 이름 부분 일치 (case-insensitive) 로 필터링한다") {
                transaction(database) {
                    repository.save(
                        basicSpace(id = SpaceId(80L), name = "팀 위키", createdAt = DUMMY_INSTANT)
                    )
                    repository.save(
                        basicSpace(
                            id = SpaceId(81L),
                            name = "공지사항",
                            createdAt = DUMMY_INSTANT.plusSeconds(1)
                        )
                    )
                    repository.save(
                        basicSpace(
                            id = SpaceId(82L),
                            name = "Team Space",
                            createdAt = DUMMY_INSTANT.plusSeconds(2)
                        )
                    )
                }

                transaction(database) {
                    val korean =
                        repository.findPage(
                            PageRequest(page = 0, size = 10),
                            privileged,
                            keyword = "위키"
                        )
                    korean.items.map { it.spaceId } shouldBe listOf(SpaceId(80L))

                    val mixedCase =
                        repository.findPage(
                            PageRequest(page = 0, size = 10),
                            privileged,
                            keyword = "team"
                        )
                    mixedCase.items.map { it.spaceId } shouldBe listOf(SpaceId(82L))
                }
            }

            it("keyword 의 SQL wildcard (%, _) 는 리터럴로 escape 된다") {
                transaction(database) {
                    repository.save(
                        basicSpace(id = SpaceId(85L), name = "100% 완료", createdAt = DUMMY_INSTANT)
                    )
                    repository.save(
                        basicSpace(
                            id = SpaceId(86L),
                            name = "그냥 이름",
                            createdAt = DUMMY_INSTANT.plusSeconds(1)
                        )
                    )
                }

                transaction(database) {
                    val result =
                        repository.findPage(
                            PageRequest(page = 0, size = 10),
                            privileged,
                            keyword = "100%"
                        )
                    result.items.map { it.spaceId } shouldBe listOf(SpaceId(85L))
                }
            }

            it("sort=NAME direction=ASC 는 이름 오름차순으로 정렬한다") {
                transaction(database) {
                    repository.save(basicSpace(id = SpaceId(90L), name = "가나다"))
                    repository.save(basicSpace(id = SpaceId(91L), name = "라마바"))
                    repository.save(basicSpace(id = SpaceId(92L), name = "사아자"))
                }

                transaction(database) {
                    val result =
                        repository.findPage(
                            PageRequest(page = 0, size = 10),
                            privileged,
                            sort = SortOption.NAME,
                            direction = SortDirection.ASC
                        )
                    result.items.map { it.spaceId } shouldBe
                        listOf(SpaceId(90L), SpaceId(91L), SpaceId(92L))
                }
            }

            it("sort=CREATED_AT direction=ASC 는 오래된 순서로 정렬한다") {
                transaction(database) {
                    repository.save(
                        basicSpace(
                            id = SpaceId(200L),
                            name = "A",
                            createdAt = DUMMY_INSTANT.plusSeconds(30)
                        )
                    )
                    repository.save(
                        basicSpace(id = SpaceId(201L), name = "B", createdAt = DUMMY_INSTANT)
                    )
                    repository.save(
                        basicSpace(
                            id = SpaceId(202L),
                            name = "C",
                            createdAt = DUMMY_INSTANT.plusSeconds(60)
                        )
                    )
                }

                transaction(database) {
                    val result =
                        repository.findPage(
                            PageRequest(page = 0, size = 10),
                            privileged,
                            sort = SortOption.CREATED_AT,
                            direction = SortDirection.ASC
                        )
                    result.items.map { it.spaceId } shouldBe
                        listOf(SpaceId(201L), SpaceId(200L), SpaceId(202L))
                }
            }

            it("sort=LAST_ACTIVITY_AT 는 각 space 의 MAX(page.updated_at) 를 사용한다") {
                val oldTime = DUMMY_INSTANT
                val midTime = DUMMY_INSTANT.plusSeconds(3600)
                val newTime = DUMMY_INSTANT.plusSeconds(7200)
                transaction(database) {
                    repository.save(basicSpace(id = SpaceId(300L), name = "A", createdAt = oldTime))
                    repository.save(basicSpace(id = SpaceId(301L), name = "B", createdAt = oldTime))
                    repository.save(basicSpace(id = SpaceId(302L), name = "C", createdAt = oldTime))

                    pageRepository.save(
                        basicPage(
                            id = PageId(1000L),
                            spaceId = SpaceId(300L),
                            createdAt = oldTime,
                            updatedAt = newTime
                        )
                    )
                    pageRepository.save(
                        basicPage(
                            id = PageId(1001L),
                            spaceId = SpaceId(301L),
                            createdAt = oldTime,
                            updatedAt = midTime
                        )
                    )
                }

                transaction(database) {
                    val result =
                        repository.findPage(
                            PageRequest(page = 0, size = 10),
                            privileged,
                            sort = SortOption.LAST_ACTIVITY_AT,
                            direction = SortDirection.DESC
                        )
                    result.items.map { it.spaceId } shouldBe
                        listOf(SpaceId(300L), SpaceId(301L), SpaceId(302L))
                    result.items.map { it.lastActivityAt } shouldBe
                        listOf(newTime, midTime, oldTime)
                }
            }

            it("sort=LAST_ACTIVITY_AT 은 soft-deleted page 를 무시한다") {
                val oldTime = DUMMY_INSTANT
                val newTime = DUMMY_INSTANT.plusSeconds(7200)
                transaction(database) {
                    repository.save(basicSpace(id = SpaceId(310L), name = "A", createdAt = oldTime))
                    pageRepository.save(
                        basicPage(
                            id = PageId(2000L),
                            spaceId = SpaceId(310L),
                            createdAt = oldTime,
                            updatedAt = newTime
                        )
                    )
                }

                transaction(database) {
                    pageRepository.delete(PageId(2000L))
                }

                transaction(database) {
                    val result =
                        repository.findPage(
                            PageRequest(page = 0, size = 10),
                            privileged
                        )
                    result.items.map { it.spaceId } shouldBe listOf(SpaceId(310L))
                    result.items.first().lastActivityAt shouldBe oldTime
                }
            }

            it("direction=ASC 는 default direction (DESC) 을 뒤집는다") {
                transaction(database) {
                    repository.save(
                        basicSpace(id = SpaceId(400L), createdAt = DUMMY_INSTANT)
                    )
                    repository.save(
                        basicSpace(
                            id = SpaceId(401L),
                            createdAt = DUMMY_INSTANT.plusSeconds(60)
                        )
                    )
                }

                transaction(database) {
                    val result =
                        repository.findPage(
                            PageRequest(page = 0, size = 10),
                            privileged,
                            sort = SortOption.LAST_ACTIVITY_AT,
                            direction = SortDirection.ASC
                        )
                    result.items.map { it.spaceId } shouldBe
                        listOf(SpaceId(400L), SpaceId(401L))
                }
            }

            it("keyword + sort 는 조합 가능 — 필터링 후 정렬") {
                transaction(database) {
                    repository.save(
                        basicSpace(id = SpaceId(500L), name = "위키 팀A", createdAt = DUMMY_INSTANT)
                    )
                    repository.save(
                        basicSpace(
                            id = SpaceId(501L),
                            name = "공지사항",
                            createdAt = DUMMY_INSTANT.plusSeconds(60)
                        )
                    )
                    repository.save(
                        basicSpace(
                            id = SpaceId(502L),
                            name = "위키 팀B",
                            createdAt = DUMMY_INSTANT.plusSeconds(120)
                        )
                    )
                }

                transaction(database) {
                    val result =
                        repository.findPage(
                            PageRequest(page = 0, size = 10),
                            privileged,
                            keyword = "위키",
                            sort = SortOption.CREATED_AT,
                            direction = SortDirection.ASC
                        )
                    result.items.map { it.spaceId } shouldBe listOf(SpaceId(500L), SpaceId(502L))
                    result.totalElements shouldBe 2L
                }
            }
        }
    })
