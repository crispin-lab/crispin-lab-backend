package com.crispinlab.space.adapter.persistence.visibility

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.compoundAnd
import org.jetbrains.exposed.v1.core.compoundOr
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList

internal fun List<VisibilityClause>.toExposedOp(): Op<Boolean> {
    check(isNotEmpty()) { "VisibilityScope.toClauses() 는 항상 최소 1 clause 를 반환한다." }
    return map { it.toOp() }.compoundOr()
}

private fun VisibilityClause.toOp(): Op<Boolean> {
    if (atoms.isEmpty()) return Op.TRUE
    return atoms.map { it.toOp() }.compoundAnd()
}

@Suppress("UNCHECKED_CAST")
private fun VisibilityAtom.toOp(): Op<Boolean> =
    when (this) {
        is VisibilityAtom.Eq<*> -> (column.dsl as Column<Any>) eq value
        is VisibilityAtom.In<*> -> (column.dsl as Column<Any>) inList values
    }
