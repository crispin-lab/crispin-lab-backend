package com.crispinlab.space.application.port.incoming.page

import com.crispinlab.common.application.UseCase
import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.incoming.page.PageSearching.Request
import com.crispinlab.space.application.port.incoming.page.PageSearching.Summary
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.SortOption
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.SortOption.Companion.asSortOption
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceId.Companion.asSpaceId
import com.crispinlab.space.domain.tag.TagId
import com.crispinlab.space.domain.tag.TagId.Companion.asTagId
import java.time.Instant

interface PageSearching : UseCase<Request, PageResult<Summary>> {
    class Request(
        keyword: String?,
        spaceId: String?,
        tagIds: List<String>,
        sort: String? = null,
        page: Int = 0,
        size: Int = DEFAULT_SIZE,
        val viewer: Viewer
    ) {
        val keyword: String? = keyword?.trim()?.takeIf { it.isNotEmpty() }
        val spaceId: SpaceId? = spaceId?.asSpaceId()
        val tagIds: List<TagId> = tagIds.map { it.asTagId() }
        val sort: SortOption = sort?.asSortOption() ?: SortOption.UPDATED_AT
        val pageRequest: PageRequest =
            PageRequest(
                page = page,
                size = size
            )
    }

    data class Summary(
        val pageId: PageId,
        val spaceId: SpaceId,
        val parentPageId: PageId?,
        val title: String,
        val displayOrder: Int,
        val updatedAt: Instant
    )
}
