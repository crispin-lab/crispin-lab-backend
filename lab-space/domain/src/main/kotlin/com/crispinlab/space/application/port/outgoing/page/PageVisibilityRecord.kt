package com.crispinlab.space.application.port.outgoing.page

import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.domain.user.UserId

data class PageVisibilityRecord(
    val pageId: PageId,
    val visibility: Visibility,
    val spaceId: SpaceId,
    val authorId: UserId
)
