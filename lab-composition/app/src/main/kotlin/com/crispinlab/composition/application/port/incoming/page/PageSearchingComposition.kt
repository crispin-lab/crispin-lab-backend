package com.crispinlab.composition.application.port.incoming.page

import com.crispinlab.common.application.UseCase
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.composition.application.port.incoming.page.PageSearchingComposition.Request
import com.crispinlab.composition.application.port.incoming.page.PageSearchingComposition.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.domain.user.UserId
import java.time.Instant

interface PageSearchingComposition : UseCase<Request, PageResult<Result>> {
    class Request(
        val keyword: String?,
        val spaceId: String?,
        val tagIds: List<String>,
        val tagName: String?,
        val sort: String?,
        val page: Int,
        val size: Int,
        val viewer: Viewer
    )

    data class Result(
        val pageId: PageId,
        val spaceId: SpaceId,
        val parentPageId: PageId?,
        val authorId: UserId,
        val authorHandle: String,
        val title: String,
        val visibility: Visibility,
        val displayOrder: Int,
        val updatedAt: Instant
    )
}
