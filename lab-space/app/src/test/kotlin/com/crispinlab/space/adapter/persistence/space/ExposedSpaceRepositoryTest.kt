package com.crispinlab.space.adapter.persistence.space

import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.testsupport.Fixtures.basicSpace
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.Instant
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExposedSpaceRepositoryTest :
    DescribeSpec({
        val database =
            Database.connect(
                url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver"
            )
        val repository = ExposedSpaceRepository()

        beforeSpec {
            transaction(database) {
                SchemaUtils.create(Spaces)
            }
        }

        afterEach {
            transaction(database) {
                Spaces.deleteAll()
            }
        }

        describe("ExposedSpaceRepository") {
            it("save 한 뒤 findBy 로 동일 entity 가 복원된다") {
                transaction(database) {
                    repository.save(
                        basicSpace(id = SpaceId(1L), name = "팀 위키", description = "공유 공간")
                    )

                    val found = repository.findBy(SpaceId(1L))

                    found.shouldNotBeNull()
                    found.id shouldBe SpaceId(1L)
                    found.name shouldBe "팀 위키"
                    found.description shouldBe "공유 공간"
                }
            }

            it("같은 ID 로 다시 save 하면 update 가 일어난다") {
                transaction(database) {
                    val space = basicSpace(id = SpaceId(2L), name = "이전")
                    repository.save(space)

                    space.update(
                        name = "새로운",
                        description = null,
                        occurredAt = Instant.parse("2026-03-01T00:00:00Z")
                    )
                    repository.save(space)

                    val found = repository.findBy(SpaceId(2L))
                    found?.name shouldBe "새로운"
                    found?.updatedAt shouldBe Instant.parse("2026-03-01T00:00:00Z")
                }
            }

            it("findAll 은 저장된 모든 행을 반환한다") {
                transaction(database) {
                    repository.save(basicSpace(id = SpaceId(3L), name = "A"))
                    repository.save(basicSpace(id = SpaceId(4L), name = "B"))

                    repository.findAll() shouldHaveSize 2
                }
            }

            it("delete 후에는 findBy 가 null 을 반환한다") {
                transaction(database) {
                    repository.save(basicSpace(id = SpaceId(5L)))
                    repository.delete(SpaceId(5L))

                    repository.findBy(SpaceId(5L)).shouldBeNull()
                }
            }
        }
    })
