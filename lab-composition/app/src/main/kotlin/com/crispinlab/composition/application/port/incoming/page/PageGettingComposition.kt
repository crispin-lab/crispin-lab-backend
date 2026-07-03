package com.crispinlab.composition.application.port.incoming.page

import com.crispinlab.common.application.UseCase
import com.crispinlab.composition.application.port.incoming.page.PageGettingComposition.Request
import com.crispinlab.composition.application.port.incoming.page.PageGettingComposition.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.domain.user.UserId
import java.time.Instant

interface PageGettingComposition : UseCase<Request, Result> {
    class Request(
        val pageId: String,
        val viewer: Viewer
    )

    data class Result(
        val pageId: PageId,
        val spaceId: SpaceId,
        val parentPageId: PageId?,
        val authorId: UserId,
        val authorHandle: String,
        val title: String,
        val content: String,
        val visibility: Visibility,
        val currentVersion: Int,
        val displayOrder: Int,
        val canEdit: Boolean,
        val canComment: Boolean,
        val createdAt: Instant,
        val updatedAt: Instant,
        val ancestors: List<AncestorSummary>
    ) {
        data class AncestorSummary(
            val pageId: PageId,
            val title: String
        )
    }
}
