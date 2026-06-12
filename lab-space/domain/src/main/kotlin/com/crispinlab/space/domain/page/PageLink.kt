package com.crispinlab.space.domain.page

import com.crispinlab.common.domain.Entity
import java.time.Instant

data class PageLink(
    override val id: PageLinkId,
    val pageId: PageId,
    val revisionId: PageRevisionId,
    val target: PageId,
    val createdAt: Instant
) : Entity<PageLinkId>
