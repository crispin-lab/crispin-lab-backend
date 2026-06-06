package com.crispinlab.space.application.port.incoming.page

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.page.PageEditing.Request
import com.crispinlab.space.application.port.incoming.page.PageEditing.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageId.Companion.asPageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.page.Visibility.Companion.asVisibility
import java.time.Instant

interface PageEditing : UseCase<Request, Result> {
    class Request(
        pageId: String,
        val title: String,
        val content: String,
        visibility: String? = null,
        val viewer: Viewer.Member
    ) {
        val pageId: PageId = pageId.asPageId()
        val visibility: Visibility? = visibility?.asVisibility()
    }

    data class Result(
        val pageId: PageId,
        val title: String,
        val version: Int,
        val updatedAt: Instant
    )
}
