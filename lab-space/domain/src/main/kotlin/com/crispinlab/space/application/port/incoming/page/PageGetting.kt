package com.crispinlab.space.application.port.incoming.page

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.page.PageGetting.Request
import com.crispinlab.space.application.port.incoming.page.PageGetting.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageId.Companion.asPageId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.domain.user.UserId
import java.time.Instant

interface PageGetting : UseCase<Request, Result> {
    class Request(
        pageId: String,
        val viewer: Viewer
    ) {
        val pageId: PageId = pageId.asPageId()
    }

    data class Result(
        val pageId: PageId,
        val spaceId: SpaceId,
        val parentPageId: PageId?,
        val authorId: UserId,
        val title: String,
        val content: String,
        val visibility: String,
        val currentVersion: Int,
        val createdAt: Instant,
        val updatedAt: Instant
    )
}
