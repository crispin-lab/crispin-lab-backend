package com.crispinlab.user.adapter.search.user

import com.crispinlab.common.persistence.PostgresTestContext
import com.crispinlab.user.adapter.persistence.user.ExposedUserRepository
import com.crispinlab.user.adapter.persistence.user.Users
import com.crispinlab.user.application.port.outgoing.user.UserSearchPort.Match
import com.crispinlab.user.domain.user.EmailAddress
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.User
import com.crispinlab.user.domain.user.UserId
import com.crispinlab.user.testsupport.Fixtures.basicUser
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

class ExposedUserSearchAdapterTest :
    DescribeSpec({
        val database = PostgresTestContext.database
        val repository = ExposedUserRepository()
        val adapter = ExposedUserSearchAdapter()

        afterEach { PostgresTestContext.truncateAll() }

        fun userWithHandle(
            id: Long,
            handle: String
        ): User =
            basicUser(
                id = UserId(id),
                email = EmailAddress("u$id@example.com"),
                handle = Handle(handle)
            )

        describe("ExposedUserSearchAdapter") {
            it("handle 부분 일치로 사용자를 찾는다") {
                transaction(database) {
                    repository.save(userWithHandle(1L, "alice"))
                    repository.save(userWithHandle(2L, "bob"))
                }

                val result = transaction(database) { adapter.search("ali", 10) }

                result shouldBe listOf(Match(userId = UserId(1L), handle = Handle("alice")))
            }

            it("중간 일치도 매칭한다") {
                transaction(database) {
                    repository.save(userWithHandle(1L, "alice_kim"))
                }

                val result = transaction(database) { adapter.search("ce_k", 10) }

                result shouldBe listOf(Match(userId = UserId(1L), handle = Handle("alice_kim")))
            }

            it("query 대소문자가 섞여 있어도 lowercase 로 매칭한다") {
                transaction(database) {
                    repository.save(userWithHandle(1L, "alice"))
                }

                val result = transaction(database) { adapter.search("ALI", 10) }

                result shouldBe listOf(Match(userId = UserId(1L), handle = Handle("alice")))
            }

            it("LIKE wildcard `%` 는 리터럴로 escape 된다") {
                transaction(database) {
                    repository.save(userWithHandle(1L, "alice"))
                    repository.save(userWithHandle(2L, "bob"))
                }

                val result = transaction(database) { adapter.search("%", 10) }

                result.shouldBeEmpty()
            }

            it("LIKE wildcard `_` 는 리터럴로 escape 되어 단일 문자 매칭으로 풀리지 않는다") {
                transaction(database) {
                    repository.save(userWithHandle(1L, "alice"))
                    repository.save(userWithHandle(2L, "alice_kim"))
                }

                val result = transaction(database) { adapter.search("e_k", 10) }

                result shouldBe listOf(Match(userId = UserId(2L), handle = Handle("alice_kim")))
            }

            it("soft delete 된 사용자는 결과에서 제외된다") {
                transaction(database) {
                    repository.save(userWithHandle(1L, "alice"))
                    repository.save(userWithHandle(2L, "alice_kim"))
                    repository.delete(UserId(2L))
                }

                val result = transaction(database) { adapter.search("ali", 10) }

                result shouldBe listOf(Match(userId = UserId(1L), handle = Handle("alice")))
            }

            it("size 상한이 결과 수를 자른다") {
                transaction(database) {
                    repository.save(userWithHandle(1L, "alice"))
                    repository.save(userWithHandle(2L, "alice_kim"))
                    repository.save(userWithHandle(3L, "alice_lee"))
                }

                val result = transaction(database) { adapter.search("ali", 2) }

                result.size shouldBe 2
            }

            it("정렬은 handle ASC, id ASC 다") {
                transaction(database) {
                    repository.save(userWithHandle(3L, "alice_lee"))
                    repository.save(userWithHandle(1L, "alice_kim"))
                    repository.save(userWithHandle(2L, "alice"))
                }

                val result = transaction(database) { adapter.search("ali", 10) }

                result shouldBe
                    listOf(
                        Match(userId = UserId(2L), handle = Handle("alice")),
                        Match(userId = UserId(1L), handle = Handle("alice_kim")),
                        Match(userId = UserId(3L), handle = Handle("alice_lee"))
                    )
            }

            it("매칭되는 사용자가 없으면 빈 리스트를 반환한다") {
                transaction(database) {
                    repository.save(userWithHandle(1L, "alice"))
                }

                val result = transaction(database) { adapter.search("zzz", 10) }

                result.shouldBeEmpty()
            }

            it("DB 의 handle row 가 손상되어 있으면 해당 row 만 누락한다") {
                transaction(database) {
                    repository.save(userWithHandle(1L, "alice"))
                    repository.save(userWithHandle(2L, "alice_kim"))
                    Users.update({ Users.id eq 2L }) {
                        it[handle] = "ALI!"
                    }
                }

                val result = transaction(database) { adapter.search("ali", 10) }

                result shouldBe listOf(Match(userId = UserId(1L), handle = Handle("alice")))
            }
        }
    })
