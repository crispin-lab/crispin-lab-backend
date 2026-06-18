package com.crispinlab.space.adapter.persistence.page

import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.domain.page.Visibility
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.or

internal fun VisibilityScope.toPagesCondition(): Op<Boolean> =
    when (this) {
        is VisibilityScope.Anonymous -> {
            Pages.visibility eq Visibility.PUBLIC.name
        }

        is VisibilityScope.Authenticated -> {
            val publicClause = Pages.visibility eq Visibility.PUBLIC.name
            val draftClause =
                (Pages.visibility eq Visibility.DRAFT.name) and
                    (Pages.authorId eq viewerId.value)
            val internalClause =
                (Pages.visibility eq Visibility.INTERNAL.name) and
                    (Pages.authorId eq viewerId.value)
            if (memberOfSpaceIds.isEmpty()) {
                publicClause or internalClause or draftClause
            } else {
                val memberClause =
                    (Pages.visibility eq Visibility.MEMBER.name) and
                        (Pages.spaceId inList memberOfSpaceIds.map { it.value })
                publicClause or memberClause or internalClause or draftClause
            }
        }

        is VisibilityScope.Privileged -> {
            Op.TRUE
        }
    }
