package com.crispinlab.space.application.port.incoming.page

import com.crispinlab.common.application.UseCase
import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.incoming.page.PageSearching.Request
import com.crispinlab.space.application.port.incoming.page.PageSearching.Summary
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceId.Companion.asSpaceId
import com.crispinlab.space.domain.tag.TagId
import com.crispinlab.space.domain.tag.TagId.Companion.asTagId
import com.crispinlab.user.domain.user.SystemRole
import com.crispinlab.user.domain.user.UserId
import java.time.Instant

interface PageSearching : UseCase<Request, PageResult<Summary>> {
    class Request(
        keyword: String?,
        spaceId: String?,
        tagIds: List<String>,
        page: Int = 0,
        size: Int = DEFAULT_SIZE,
        val currentUserId: UserId?,
        val currentUserRole: SystemRole?
    ) {
        val keyword: String? = keyword?.trim()?.takeIf { it.isNotEmpty() }
        val spaceId: SpaceId? = spaceId?.asSpaceId()
        val tagIds: List<TagId> = tagIds.map { it.asTagId() }
        val pageRequest: PageRequest =
            PageRequest(
                page = page,
                size = size
            )
    }

    data class Summary(
        val pageId: PageId,
        val spaceId: SpaceId,
        val title: String,
        val updatedAt: Instant
    )
}
