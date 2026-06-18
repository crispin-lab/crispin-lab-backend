package com.crispinlab.space.adapter.persistence.page

import com.crispinlab.space.adapter.persistence.space.decodeSpaceVisibility
import com.crispinlab.space.application.port.outgoing.page.PageAncestorPort
import com.crispinlab.space.application.port.outgoing.page.PageAncestorPort.Ancestor
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.domain.user.UserId
import org.jetbrains.exposed.v1.core.LongColumnType
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.springframework.stereotype.Repository

@Repository
class ExposedPageAncestorAdapter : PageAncestorPort {
    override fun findAncestorsOf(pageId: PageId): List<Ancestor> =
        buildList {
            TransactionManager.current().exec(
                stmt = ANCESTOR_CHAIN_SQL,
                args = listOf(LongColumnType() to pageId.value),
                explicitStatementType = StatementType.SELECT
            ) { rs ->
                while (rs.next()) {
                    add(
                        Ancestor(
                            pageId = PageId(rs.getLong("id")),
                            title = rs.getString("title"),
                            spaceId = SpaceId(rs.getLong("space_id")),
                            spaceVisibility =
                                decodeSpaceVisibility(
                                    rs.getString("space_visibility")
                                ),
                            authorId = UserId(rs.getLong("author_id")),
                            visibility = decodeVisibility(rs.getString("visibility"))
                        )
                    )
                }
            }
        }

    companion object {
        private const val MAX_ANCESTORS: Int = 64

        private val ANCESTOR_CHAIN_SQL =
            """
            WITH RECURSIVE ancestor_chain AS (
                SELECT p.id, p.title, p.parent_page_id, p.visibility, p.space_id, p.author_id,
                       s.visibility AS space_visibility, 1 AS depth
                FROM pages p
                INNER JOIN pages target ON target.id = ?
                                        AND target.deleted_at IS NULL
                                        AND p.id = target.parent_page_id
                                        AND p.space_id = target.space_id
                INNER JOIN spaces s ON s.id = p.space_id
                                    AND s.deleted_at IS NULL
                WHERE p.deleted_at IS NULL

                UNION ALL

                SELECT p.id, p.title, p.parent_page_id, p.visibility, p.space_id, p.author_id,
                       s.visibility AS space_visibility, ac.depth + 1
                FROM pages p
                INNER JOIN ancestor_chain ac ON p.id = ac.parent_page_id
                                            AND p.space_id = ac.space_id
                INNER JOIN spaces s ON s.id = p.space_id
                                    AND s.deleted_at IS NULL
                WHERE p.deleted_at IS NULL
                  AND ac.depth < $MAX_ANCESTORS
            )
            SELECT id, title, visibility, space_id, space_visibility, author_id
            FROM ancestor_chain
            ORDER BY depth DESC
            """.trimIndent()
    }
}
