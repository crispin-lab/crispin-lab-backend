package com.crispinlab.space.application.port.incoming.tag

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.tag.PageTagAttaching.Request
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageId.Companion.asPageId
import com.crispinlab.space.domain.tag.TagId
import com.crispinlab.space.domain.tag.TagId.Companion.asTagId
import com.crispinlab.space.domain.user.UserId

interface PageTagAttaching : UseCase<Request, Unit> {
    class Request(
        pageId: String,
        tagId: String,
        val currentUserId: UserId
    ) {
        val pageId: PageId = pageId.asPageId()
        val tagId: TagId = tagId.asTagId()
    }
}
