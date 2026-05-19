package com.crispinlab.common.persistence

import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.testcontainers.containers.PostgreSQLContainer

object PostgresTestContext {
    val container: PostgreSQLContainer<*> =
        PostgreSQLContainer<Nothing>("postgres:16")
            .apply {
                start()
            }

    val database: Database =
        Database.connect(
            url = container.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = container.username,
            password = container.password
        )

    private val userTables: String =
        run {
            migrate()
            loadUserTables()
        }

    fun truncateAll() {
        if (userTables.isBlank()) return
        transaction(database) {
            exec("TRUNCATE TABLE $userTables RESTART IDENTITY CASCADE")
        }
    }

    private fun migrate() {
        runCatching {
            Flyway
                .configure()
                .dataSource(container.jdbcUrl, container.username, container.password)
                .locations("classpath:db/migration")
                .load()
                .migrate()
        }.onFailure { cause ->
            throw IllegalStateException(
                "Flyway 마이그레이션 적용 실패 — db/migration SQL 또는 컨테이너 상태 확인 필요.",
                cause
            )
        }
    }

    private fun loadUserTables(): String =
        transaction(database) {
            exec(
                "SELECT string_agg(quote_ident(table_name), ',') " +
                    "FROM information_schema.tables " +
                    "WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history'"
            ) { rs ->
                if (rs.next()) rs.getString(1) else null
            }.orEmpty()
        }
}
