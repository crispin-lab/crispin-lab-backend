package com.crispinlab.space.application.port.incoming.page

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.page.PageEditing.Request
import com.crispinlab.space.application.port.incoming.page.PageEditing.Result
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageId.Companion.asPageId
import com.crispinlab.space.domain.user.UserId
import java.time.Instant

interface PageEditing : UseCase<Request, Result> {
    class Request(
        pageId: String,
        val title: String,
        val content: String,
        val currentUserId: UserId
    ) {
        val pageId: PageId = pageId.asPageId()
    }

    data class Result(
        val pageId: String,
        val title: String,
        val version: Int,
        val updatedAt: Instant
    )
}
