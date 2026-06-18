package com.crispinlab.space.adapter.persistence.page

import com.crispinlab.space.adapter.persistence.space.Spaces
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceVisibility
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.or

internal fun VisibilityScope.toPagesCondition(): Op<Boolean> =
    when (this) {
        is VisibilityScope.Anonymous -> {
            (Pages.visibility eq Visibility.PUBLIC.name) and
                (Spaces.visibility eq SpaceVisibility.PUBLIC.name)
        }

        is VisibilityScope.Authenticated -> {
            val publicEff =
                (Pages.visibility eq Visibility.PUBLIC.name) and
                    (Spaces.visibility eq SpaceVisibility.PUBLIC.name)
            val internalEff =
                (
                    (Pages.visibility eq Visibility.INTERNAL.name) or
                        (
                            (
                                Pages.visibility inList
                                    listOf(Visibility.PUBLIC.name, Visibility.MEMBER.name)
                            ) and
                                (Spaces.visibility eq SpaceVisibility.INTERNAL.name)
                        )
                ) and
                    (Pages.authorId eq viewerId.value)
            val draftEff =
                (Pages.visibility eq Visibility.DRAFT.name) and
                    (Pages.authorId eq viewerId.value)
            if (memberOfSpaceIds.isEmpty()) {
                publicEff or internalEff or draftEff
            } else {
                val memberEff =
                    (Pages.visibility eq Visibility.MEMBER.name) and
                        (Spaces.visibility eq SpaceVisibility.PUBLIC.name) and
                        (Pages.spaceId inList memberOfSpaceIds.map { it.value })
                publicEff or memberEff or internalEff or draftEff
            }
        }

        is VisibilityScope.Privileged -> {
            Op.TRUE
        }
    }
