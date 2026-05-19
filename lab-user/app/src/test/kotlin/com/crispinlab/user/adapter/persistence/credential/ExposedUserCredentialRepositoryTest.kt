package com.crispinlab.user.adapter.persistence.credential

import com.crispinlab.common.persistence.PostgresTestContext
import com.crispinlab.user.adapter.persistence.user.ExposedUserRepository
import com.crispinlab.user.domain.credential.Credential
import com.crispinlab.user.domain.credential.PasswordHash
import com.crispinlab.user.domain.credential.UserCredentialId
import com.crispinlab.user.domain.user.UserId
import com.crispinlab.user.testsupport.Fixtures.basicUser
import com.crispinlab.user.testsupport.Fixtures.basicUserCredential
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.sql.SQLException
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

class ExposedUserCredentialRepositoryTest :
    DescribeSpec({
        val database = PostgresTestContext.database
        val repository = ExposedUserCredentialRepository()
        val userRepository = ExposedUserRepository()

        afterEach {
            PostgresTestContext.truncateAll()
        }

        describe("ExposedUserCredentialRepository") {
            it("PASSWORD 자격증명을 save 한 뒤 findBy 로 복원할 수 있다") {
                val hash = PasswordHash("\$2a\$12\$" + "h".repeat(53))

                transaction(database) {
                    repository.save(
                        basicUserCredential(
                            id = UserCredentialId(1L),
                            userId = UserId(100L),
                            credential = Credential.Password(hash)
                        )
                    )
                }

                transaction(database) {
                    val found = repository.findBy(UserCredentialId(1L)).shouldNotBeNull()
                    found.userId shouldBe UserId(100L)
                    found.credential.shouldBeInstanceOf<Credential.Password>()
                    (found.credential as Credential.Password).hash shouldBe hash
                }
            }

            it("findPasswordBy 는 사용자별 PASSWORD 자격증명을 가져온다") {
                transaction(database) {
                    userRepository.save(basicUser(id = UserId(200L)))
                    repository.save(
                        basicUserCredential(
                            id = UserCredentialId(10L),
                            userId = UserId(200L)
                        )
                    )
                }

                transaction(database) {
                    val found = repository.findPasswordBy(UserId(200L)).shouldNotBeNull()
                    found.id shouldBe UserCredentialId(10L)
                }
            }

            it("findPasswordBy 는 사용자가 없으면 null 을 반환한다") {
                transaction(database) {
                    repository.findPasswordBy(UserId(999L)).shouldBeNull()
                }
            }

            it("findPasswordBy 는 soft deleted 사용자의 자격증명을 제외한다") {
                transaction(database) {
                    userRepository.save(basicUser(id = UserId(210L)))
                    repository.save(
                        basicUserCredential(
                            id = UserCredentialId(15L),
                            userId = UserId(210L)
                        )
                    )
                    userRepository.delete(UserId(210L))
                }

                transaction(database) {
                    repository.findPasswordBy(UserId(210L)).shouldBeNull()
                }
            }

            it("repository.delete 는 hard delete — row 자체가 사라진다") {
                transaction(database) {
                    repository.save(basicUserCredential(id = UserCredentialId(20L)))
                }

                transaction(database) {
                    repository.delete(UserCredentialId(20L))
                }

                transaction(database) {
                    repository.findBy(UserCredentialId(20L)).shouldBeNull()
                    UserCredentials
                        .selectAll()
                        .where { UserCredentials.id eq 20L }
                        .firstOrNull()
                        .shouldBeNull()
                }
            }

            it("같은 user 에 PASSWORD 자격증명을 두 번 저장하면 partial unique 위반으로 실패한다") {
                transaction(database) {
                    repository.save(
                        basicUserCredential(
                            id = UserCredentialId(30L),
                            userId = UserId(300L)
                        )
                    )
                }

                shouldThrow<SQLException> {
                    transaction(database) {
                        repository.save(
                            basicUserCredential(
                                id = UserCredentialId(31L),
                                userId = UserId(300L)
                            )
                        )
                    }
                }
            }

            it("저장된 PASSWORD 자격증명의 password_hash 가 손상되면 IllegalStateException 으로 매핑된다") {
                transaction(database) {
                    repository.save(basicUserCredential(id = UserCredentialId(40L)))
                    UserCredentials.update({ UserCredentials.id eq 40L }) {
                        it[passwordHash] = null
                    }
                }

                shouldThrow<IllegalStateException> {
                    transaction(database) {
                        repository.findBy(UserCredentialId(40L))
                    }
                }
            }
        }
    })
