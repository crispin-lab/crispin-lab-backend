package com.crispinlab.space.adapter.persistence.visibility

import com.crispinlab.space.adapter.persistence.page.Pages
import com.crispinlab.space.adapter.persistence.space.Spaces
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceVisibility
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.IColumnType

internal sealed interface VisibilityColumn<T : Any> {
    val dsl: Column<T>
    val sqlName: String get() = "${dsl.table.tableName}.${dsl.name}"
    val columnType: IColumnType<T> get() = dsl.columnType

    data object PageVisibility : VisibilityColumn<String> {
        override val dsl: Column<String> = Pages.visibility
    }

    data object SpaceVisibility : VisibilityColumn<String> {
        override val dsl: Column<String> = Spaces.visibility
    }

    data object PageAuthorId : VisibilityColumn<Long> {
        override val dsl: Column<Long> = Pages.authorId
    }

    data object PageSpaceId : VisibilityColumn<Long> {
        override val dsl: Column<Long> = Pages.spaceId
    }
}

internal sealed interface VisibilityAtom {
    val column: VisibilityColumn<*>

    data class Eq<T : Any>(
        override val column: VisibilityColumn<T>,
        val value: T
    ) : VisibilityAtom

    data class In<T : Any>(
        override val column: VisibilityColumn<T>,
        val values: List<T>
    ) : VisibilityAtom
}

internal data class VisibilityClause(
    val atoms: List<VisibilityAtom>
) {
    companion object {
        val ALWAYS: VisibilityClause = VisibilityClause(emptyList())
    }
}

internal fun VisibilityScope.toClauses(): List<VisibilityClause> =
    when (this) {
        is VisibilityScope.Anonymous -> listOf(anonymousClause)
        is VisibilityScope.Authenticated -> authenticatedClauses()
        is VisibilityScope.Privileged -> listOf(VisibilityClause.ALWAYS)
    }

private val anonymousClause: VisibilityClause =
    VisibilityClause(
        atoms =
            listOf(
                VisibilityAtom.Eq(VisibilityColumn.PageVisibility, Visibility.PUBLIC.name),
                VisibilityAtom.Eq(VisibilityColumn.SpaceVisibility, SpaceVisibility.PUBLIC.name)
            )
    )

private fun VisibilityScope.Authenticated.authenticatedClauses(): List<VisibilityClause> =
    buildList {
        add(anonymousClause)
        if (memberOfSpaceIds.isNotEmpty()) {
            add(
                VisibilityClause(
                    atoms =
                        listOf(
                            VisibilityAtom.Eq(
                                column = VisibilityColumn.PageVisibility,
                                value = Visibility.MEMBER.name
                            ),
                            VisibilityAtom.Eq(
                                column = VisibilityColumn.SpaceVisibility,
                                value = SpaceVisibility.PUBLIC.name
                            ),
                            VisibilityAtom.In(
                                column = VisibilityColumn.PageSpaceId,
                                values = memberOfSpaceIds.map { it.value }
                            )
                        )
                )
            )
        }
        add(
            VisibilityClause(
                atoms =
                    listOf(
                        VisibilityAtom.Eq(
                            column = VisibilityColumn.PageVisibility,
                            value = Visibility.INTERNAL.name
                        ),
                        VisibilityAtom.Eq(
                            column = VisibilityColumn.PageAuthorId,
                            value = viewerId.value
                        )
                    )
            )
        )
        add(
            VisibilityClause(
                atoms =
                    listOf(
                        VisibilityAtom.In(
                            column = VisibilityColumn.PageVisibility,
                            values = listOf(Visibility.PUBLIC.name, Visibility.MEMBER.name)
                        ),
                        VisibilityAtom.Eq(
                            column = VisibilityColumn.SpaceVisibility,
                            value = SpaceVisibility.INTERNAL.name
                        ),
                        VisibilityAtom.Eq(
                            column = VisibilityColumn.PageAuthorId,
                            value = viewerId.value
                        )
                    )
            )
        )
        add(
            VisibilityClause(
                atoms =
                    listOf(
                        VisibilityAtom.Eq(
                            column = VisibilityColumn.PageVisibility,
                            value = Visibility.DRAFT.name
                        ),
                        VisibilityAtom.Eq(
                            column = VisibilityColumn.PageAuthorId,
                            value = viewerId.value
                        )
                    )
            )
        )
    }
