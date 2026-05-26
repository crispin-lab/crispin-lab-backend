package com.crispinlab.space.application.port.incoming.page

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.page.PageRevisionGetting.Request
import com.crispinlab.space.application.port.incoming.page.PageRevisionGetting.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageId.Companion.asPageId
import com.crispinlab.space.domain.page.PageRevisionId
import com.crispinlab.user.domain.user.UserId
import java.time.Instant

interface PageRevisionGetting : UseCase<Request, Result> {
    class Request(
        pageId: String,
        val version: Int,
        val viewer: Viewer
    ) {
        val pageId: PageId = pageId.asPageId()

        init {
            require(version >= 1) {
                "리비전 버전은 1 이상이어야 합니다."
            }
        }
    }

    data class Result(
        val revisionId: PageRevisionId,
        val pageId: PageId,
        val version: Int,
        val title: String,
        val content: String,
        val authorId: UserId,
        val createdAt: Instant
    )
}
