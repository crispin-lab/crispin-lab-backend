package com.crispinlab.space.adapter.persistence.space

import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.testsupport.Fixtures.basicSpace
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
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

            it("delete 후에는 findBy 가 null 을 반환한다") {
                transaction(database) {
                    repository.save(basicSpace(id = SpaceId(5L)))
                }

                transaction(database) {
                    repository.delete(SpaceId(5L))
                }

                transaction(database) {
                    repository.findBy(SpaceId(5L)).shouldBeNull()
                }
            }
        }
    })
