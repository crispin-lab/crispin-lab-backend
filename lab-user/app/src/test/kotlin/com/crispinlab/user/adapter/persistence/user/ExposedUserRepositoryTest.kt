package com.crispinlab.user.adapter.persistence.user

import com.crispinlab.common.persistence.PostgresTestContext
import com.crispinlab.user.domain.user.EmailAddress
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.SystemRole
import com.crispinlab.user.domain.user.UserId
import com.crispinlab.user.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.user.testsupport.Fixtures.basicUser
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

class ExposedUserRepositoryTest :
    DescribeSpec({
        val database = PostgresTestContext.database
        val repository = ExposedUserRepository()

        afterEach {
            PostgresTestContext.truncateAll()
        }

        describe("ExposedUserRepository") {
            it("save 한 뒤 별도 transaction 의 findBy 로 동일 entity 가 복원된다") {
                transaction(database) {
                    repository.save(
                        basicUser(
                            id = UserId(1L),
                            email = EmailAddress("alice@example.com"),
                            handle = Handle("alice")
                        )
                    )
                }

                transaction(database) {
                    val found = repository.findBy(UserId(1L))

                    found.shouldNotBeNull()
                    found.id shouldBe UserId(1L)
                    found.email shouldBe EmailAddress("alice@example.com")
                    found.handle shouldBe Handle("alice")
                    found.role shouldBe SystemRole.USER
                }
            }

            it("같은 ID 로 다시 save 하면 update 가 일어난다") {
                transaction(database) {
                    repository.save(basicUser(id = UserId(2L), handle = Handle("old_handle")))
                }

                transaction(database) {
                    val existing = repository.findBy(UserId(2L))!!
                    existing.changeHandle(Handle("new_handle"))
                    repository.save(existing)
                }

                transaction(database) {
                    repository.findBy(UserId(2L))?.handle shouldBe Handle("new_handle")
                }
            }

            it("findByEmail 로 이메일 기반 조회가 동작한다") {
                transaction(database) {
                    repository.save(
                        basicUser(
                            id = UserId(3L),
                            email = EmailAddress("bob@example.com"),
                            handle = Handle("bob")
                        )
                    )
                }

                transaction(database) {
                    val found = repository.findByEmail(EmailAddress("bob@example.com"))

                    found.shouldNotBeNull()
                    found.id shouldBe UserId(3L)
                }
            }

            it("repository.delete 는 soft delete 로 동작 — row 는 보존되고 findBy 는 null 을 반환한다") {
                transaction(database) {
                    repository.save(basicUser(id = UserId(5L)))
                }

                transaction(database) {
                    repository.delete(UserId(5L))
                }

                transaction(database) {
                    repository.findBy(UserId(5L)).shouldBeNull()
                    val row =
                        Users
                            .selectAll()
                            .where { Users.id eq 5L }
                            .firstOrNull()
                            .shouldNotBeNull()
                    row[Users.deletedAt].shouldNotBeNull()
                }
            }

            it("findByEmail 은 soft deleted 사용자를 제외한다") {
                transaction(database) {
                    repository.save(
                        basicUser(
                            id = UserId(6L),
                            email = EmailAddress("deleted@example.com"),
                            handle = Handle("deleted")
                        )
                    )
                    repository.delete(UserId(6L))
                }

                transaction(database) {
                    repository.findByEmail(EmailAddress("deleted@example.com")).shouldBeNull()
                }
            }

            it("save 가 soft delete 된 row 의 deleted_at 을 덮지 않는다") {
                val originalDeletedAt =
                    transaction(database) {
                        repository.save(basicUser(id = UserId(100L)))
                        repository.delete(UserId(100L))
                        Users
                            .selectAll()
                            .where { Users.id eq 100L }
                            .first()[Users.deletedAt]
                    }.shouldNotBeNull()

                transaction(database) {
                    repository.save(
                        basicUser(id = UserId(100L), handle = Handle("revived"), deletedAt = null)
                    )
                }

                transaction(database) {
                    val row =
                        Users
                            .selectAll()
                            .where { Users.id eq 100L }
                            .first()
                    row[Users.deletedAt] shouldBe originalDeletedAt
                    repository.findBy(UserId(100L)).shouldBeNull()
                }
            }

            it("save 는 immutable 컬럼 (createdAt) 을 덮지 않는다") {
                transaction(database) {
                    repository.save(
                        basicUser(id = UserId(110L), createdAt = DUMMY_INSTANT)
                    )
                }

                transaction(database) {
                    repository.save(
                        basicUser(
                            id = UserId(110L),
                            handle = Handle("modified"),
                            createdAt = DUMMY_INSTANT.plusSeconds(60)
                        )
                    )
                }

                transaction(database) {
                    val found = repository.findBy(UserId(110L)).shouldNotBeNull()
                    found.handle shouldBe Handle("modified")
                    found.createdAt shouldBe DUMMY_INSTANT
                }
            }

            it("DB 의 role 값이 손상되어 있으면 IllegalStateException 으로 매핑된다") {
                transaction(database) {
                    repository.save(basicUser(id = UserId(200L)))
                    Users.update({ Users.id eq 200L }) {
                        it[role] = "UNKNOWN_ROLE"
                    }
                }

                shouldThrow<IllegalStateException> {
                    transaction(database) {
                        repository.findBy(UserId(200L))
                    }
                }
            }
        }
    })
