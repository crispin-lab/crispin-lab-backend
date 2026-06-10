package com.crispinlab.user.adapter.persistence.user

import com.crispinlab.common.persistence.PostgresTestContext
import com.crispinlab.user.domain.user.EmailAddress
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.UserId
import com.crispinlab.user.testsupport.Fixtures.basicUser
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExposedUserHandleQueryAdapterTest :
    DescribeSpec({
        val database = PostgresTestContext.database
        val repository = ExposedUserRepository()
        val handleQuery = ExposedUserHandleQueryAdapter()

        afterEach {
            PostgresTestContext.truncateAll()
        }

        describe("ExposedUserHandleQueryAdapter") {
            it("여러 UserId 를 한 번에 조회해 Map 으로 반환한다") {
                transaction(database) {
                    repository.save(
                        basicUser(
                            id = UserId(1L),
                            email = EmailAddress("alice@example.com"),
                            handle = Handle("alice")
                        )
                    )
                    repository.save(
                        basicUser(
                            id = UserId(2L),
                            email = EmailAddress("bob@example.com"),
                            handle = Handle("bob")
                        )
                    )
                    repository.save(
                        basicUser(
                            id = UserId(3L),
                            email = EmailAddress("carol@example.com"),
                            handle = Handle("carol")
                        )
                    )
                }

                val result =
                    transaction(database) {
                        handleQuery.handlesOf(listOf(UserId(1L), UserId(2L), UserId(3L)))
                    }

                result shouldBe
                    mapOf(
                        UserId(1L) to Handle("alice"),
                        UserId(2L) to Handle("bob"),
                        UserId(3L) to Handle("carol")
                    )
            }

            it("soft delete 된 사용자는 결과에서 누락된다") {
                transaction(database) {
                    repository.save(
                        basicUser(
                            id = UserId(10L),
                            email = EmailAddress("active@example.com"),
                            handle = Handle("active")
                        )
                    )
                    repository.save(
                        basicUser(
                            id = UserId(11L),
                            email = EmailAddress("removed@example.com"),
                            handle = Handle("removed")
                        )
                    )
                    repository.delete(UserId(11L))
                }

                val result =
                    transaction(database) {
                        handleQuery.handlesOf(listOf(UserId(10L), UserId(11L)))
                    }

                result.keys.shouldContainExactlyInAnyOrder(UserId(10L))
                result[UserId(10L)] shouldBe Handle("active")
            }

            it("빈 Collection 으로 호출하면 DB 조회 없이 빈 Map 을 반환한다") {
                val result = handleQuery.handlesOf(emptyList())

                result.shouldBeEmpty()
            }

            it("중복 id 와 존재하지 않는 id 가 섞여 있어도 존재하는 사용자만 매핑한다") {
                transaction(database) {
                    repository.save(basicUser(id = UserId(20L), handle = Handle("only_one")))
                }

                val result =
                    transaction(database) {
                        handleQuery.handlesOf(
                            listOf(UserId(20L), UserId(20L), UserId(999L))
                        )
                    }

                result shouldBe mapOf(UserId(20L) to Handle("only_one"))
            }
        }
    })
