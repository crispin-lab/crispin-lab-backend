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
import com.crispinlab.space.domain.page.PageId.Companion.asPageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceId.Companion.asSpaceId
import com.crispinlab.space.domain.tag.TagId
import com.crispinlab.space.domain.tag.TagId.Companion.asTagId
import com.crispinlab.user.domain.user.UserId
import java.time.Instant

interface PageSearching : UseCase<Request, PageResult<Summary>> {
    class Request(
        keyword: String?,
        spaceId: String?,
        tagIds: List<String>,
        tagName: String? = null,
        sort: String? = null,
        parentPageId: String? = null,
        val onlyRoot: Boolean = false,
        page: Int = 0,
        size: Int = DEFAULT_SIZE,
        val viewer: Viewer
    ) {
        val keyword: String? = keyword?.trim()?.takeIf { it.isNotEmpty() }
        val spaceId: SpaceId? = spaceId?.asSpaceId()
        val tagIds: List<TagId> = tagIds.map { it.asTagId() }
        val tagName: String? = tagName?.trim()?.takeIf { it.isNotEmpty() }
        val sort: SortOption = sort?.asSortOption() ?: SortOption.UPDATED_AT
        val parentPageId: PageId? = parentPageId?.asPageId()
        val pageRequest: PageRequest =
            PageRequest(
                page = page,
                size = size
            )

        init {
            require(!(onlyRoot && this.parentPageId != null)) {
                "parentPageId 와 onlyRoot 는 동시에 지정할 수 없습니다."
            }
        }
    }

    data class Summary(
        val pageId: PageId,
        val spaceId: SpaceId,
        val parentPageId: PageId?,
        val authorId: UserId,
        val title: String,
        val visibility: Visibility,
        val displayOrder: Int,
        val updatedAt: Instant
    )
}
