package com.crispinlab.space.application.port.incoming.page

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.page.PageMoving.Request
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageId.Companion.asPageId

interface PageMoving : UseCase<Request, Unit> {
    class Request(
        pageId: String,
        parentPageId: String?,
        val viewer: Viewer.Member
    ) {
        val pageId: PageId = pageId.asPageId()
        val parentPageId: PageId? = parentPageId?.asPageId()
    }
}
