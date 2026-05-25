package com.crispinlab.space.application.port.incoming.tag

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.tag.PageTagAttaching.Request
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageId.Companion.asPageId
import com.crispinlab.space.domain.tag.TagId
import com.crispinlab.space.domain.tag.TagId.Companion.asTagId

interface PageTagAttaching : UseCase<Request, Unit> {
    class Request(
        pageId: String,
        tagId: String,
        val viewer: Viewer.Member
    ) {
        val pageId: PageId = pageId.asPageId()
        val tagId: TagId = tagId.asTagId()
    }
}
