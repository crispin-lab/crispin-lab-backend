package com.crispinlab.space.adapter.persistence.visibility

import org.jetbrains.exposed.v1.core.IColumnType

internal data class VisibilityFragment(
    val sql: String,
    val args: List<Pair<IColumnType<*>, Any?>>
)

internal fun List<VisibilityClause>.toSqlFragment(): VisibilityFragment {
    check(isNotEmpty()) { "VisibilityScope.toClauses() 는 항상 최소 1 clause 를 반환한다." }
    val clauses = map { it.toFragment() }
    return VisibilityFragment(
        sql = clauses.joinToString(separator = " OR ", prefix = "(", postfix = ")") { it.sql },
        args = clauses.flatMap { it.args }
    )
}

private fun VisibilityClause.toFragment(): VisibilityFragment {
    if (atoms.isEmpty()) return VisibilityFragment(sql = "TRUE", args = emptyList())
    val fragments = atoms.map { it.toFragment() }
    return VisibilityFragment(
        sql = fragments.joinToString(separator = " AND ", prefix = "(", postfix = ")") { it.sql },
        args = fragments.flatMap { it.args }
    )
}

private fun VisibilityAtom.toFragment(): VisibilityFragment =
    when (this) {
        is VisibilityAtom.Eq<*> -> {
            VisibilityFragment(
                sql = "${column.sqlName} = ?",
                args = listOf(column.columnType to value)
            )
        }

        is VisibilityAtom.In<*> -> {
            val placeholders = values.joinToString(", ") { "?" }
            VisibilityFragment(
                sql = "${column.sqlName} IN ($placeholders)",
                args = values.map { column.columnType to it }
            )
        }
    }
