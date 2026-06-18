package com.crispinlab.space.application.port.outgoing.page

import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.user.domain.user.UserId

interface PageAncestorPort {
    fun findAncestorsOf(pageId: PageId): List<Ancestor>

    data class Ancestor(
        val pageId: PageId,
        val title: String,
        val spaceId: SpaceId,
        val spaceVisibility: SpaceVisibility,
        val authorId: UserId,
        val visibility: Visibility
    )
}
