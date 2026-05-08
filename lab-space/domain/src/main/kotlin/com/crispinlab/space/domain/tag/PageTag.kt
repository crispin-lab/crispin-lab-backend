package com.crispinlab.space.domain.tag

import com.crispinlab.space.domain.page.PageId
import java.time.Instant

data class PageTag(
    val pageId: PageId,
    val tagId: TagId,
    val createdAt: Instant
)
