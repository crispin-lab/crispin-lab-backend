package com.crispinlab.space.application.port.incoming.page

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.page.PageReordering.Request
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageId.Companion.asPageId

interface PageReordering : UseCase<Request, Unit> {
    class Request(
        pageId: String,
        val displayOrder: Int,
        val viewer: Viewer.Member
    ) {
        val pageId: PageId = pageId.asPageId()

        init {
            require(displayOrder >= 0) {
                "표시 순서는 0 이상이어야 합니다."
            }
        }
    }
}
