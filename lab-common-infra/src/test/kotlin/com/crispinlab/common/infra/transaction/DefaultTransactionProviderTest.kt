package com.crispinlab.common.infra.transaction

import com.crispinlab.common.transaction.TransactionProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import javax.sql.DataSource
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionSynchronizationManager.isCurrentTransactionReadOnly

class DefaultTransactionProviderTest :
    DescribeSpec({
        val context = AnnotationConfigApplicationContext(TestConfig::class.java)
        val provider = context.getBean(TransactionProvider::class.java)
        val jdbcTemplate = context.getBean(JdbcTemplate::class.java)

        beforeSpec {
            jdbcTemplate.execute("DROP TABLE IF EXISTS counter")
            jdbcTemplate.execute("CREATE TABLE counter (id INT PRIMARY KEY, amount INT)")
            jdbcTemplate.update("INSERT INTO counter (id, amount) VALUES (1, 0)")
        }
        afterSpec {
            context.close()
        }
        beforeEach {
            jdbcTemplate.update("UPDATE counter SET amount = 0 WHERE id = 1")
        }

        describe("transactional") {
            it("정상 흐름에서 커밋되고 block 반환값이 그대로 흐른다") {
                val result =
                    provider.transactional {
                        jdbcTemplate.update("UPDATE counter SET amount = 7 WHERE id = 1")
                        "ok"
                    }

                result shouldBe "ok"
                currentValue(jdbcTemplate) shouldBe 7
            }

            it("block 안에서 예외가 나면 롤백된다") {
                shouldThrow<IllegalStateException> {
                    provider.transactional {
                        jdbcTemplate.update("UPDATE counter SET amount = 99 WHERE id = 1")
                        error("boom")
                    }
                }

                currentValue(jdbcTemplate) shouldBe 0
            }

            it("readOnly = true 가 현재 트랜잭션에 반영된다") {
                val isReadOnly =
                    provider.transactional(readOnly = true) {
                        isCurrentTransactionReadOnly()
                    }

                isReadOnly shouldBe true
            }

            it("readOnly 기본값(false)에서는 read-only 가 아니다") {
                val isReadOnly =
                    provider.transactional {
                        isCurrentTransactionReadOnly()
                    }

                isReadOnly shouldBe false
            }
        }
    }) {
    @Configuration
    class TestConfig {
        @Bean
        fun dataSource(): DataSource =
            DriverManagerDataSource(
                "jdbc:h2:mem:tx-provider-test;DB_CLOSE_DELAY=-1",
                "sa",
                ""
            )

        @Bean
        fun transactionManager(dataSource: DataSource): PlatformTransactionManager =
            DataSourceTransactionManager(dataSource)

        @Bean
        fun jdbcTemplate(dataSource: DataSource): JdbcTemplate = JdbcTemplate(dataSource)

        @Bean
        fun transactionProvider(
            transactionManager: PlatformTransactionManager
        ): TransactionProvider = DefaultTransactionProvider(transactionManager)
    }

    companion object {
        private fun currentValue(jdbcTemplate: JdbcTemplate): Int =
            jdbcTemplate.queryForObject("SELECT amount FROM counter WHERE id = 1", Int::class.java)
                ?: error("counter row missing")
    }
}
