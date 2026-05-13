package com.crispinlab.space.application.port.incoming.page

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.page.PageGetting.Request
import com.crispinlab.space.application.port.incoming.page.PageGetting.Result
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageId.Companion.asPageId
import com.crispinlab.space.domain.user.UserId
import java.time.Instant

interface PageGetting : UseCase<Request, Result> {
    class Request(
        pageId: String,
        val currentUserId: UserId
    ) {
        val pageId: PageId = pageId.asPageId()
    }

    data class Result(
        val pageId: String,
        val spaceId: String,
        val parentPageId: String?,
        val authorId: String,
        val title: String,
        val content: String,
        val visibility: String,
        val currentVersion: Int,
        val createdAt: Instant,
        val updatedAt: Instant
    )
}
