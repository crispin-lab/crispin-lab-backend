package com.crispinlab.space.application.port.incoming.page

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.page.PageDeleting.Request
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageId.Companion.asPageId
import com.crispinlab.user.domain.user.AuthContext

interface PageDeleting : UseCase<Request, Unit> {
    class Request(
        pageId: String,
        val auth: AuthContext.Authenticated
    ) {
        val pageId: PageId = pageId.asPageId()
    }
}
