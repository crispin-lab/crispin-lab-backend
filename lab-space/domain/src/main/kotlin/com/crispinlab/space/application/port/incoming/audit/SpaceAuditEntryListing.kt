package com.crispinlab.space.application.port.incoming.audit

import com.crispinlab.common.application.UseCase
import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.incoming.audit.SpaceAuditEntryListing.Request
import com.crispinlab.space.application.port.incoming.audit.SpaceAuditEntryListing.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.audit.SpaceAuditAction
import com.crispinlab.space.domain.audit.SpaceAuditEntryId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceId.Companion.asSpaceId
import com.crispinlab.user.domain.user.UserId
import java.time.Instant

interface SpaceAuditEntryListing : UseCase<Request, PageResult<Result>> {
    class Request(
        spaceId: String,
        page: Int = 0,
        size: Int = PageRequest.DEFAULT_SIZE,
        val viewer: Viewer.Member
    ) {
        val spaceId: SpaceId = spaceId.asSpaceId()
        val pageRequest: PageRequest = PageRequest(page = page, size = size)
    }

    data class Result(
        val id: SpaceAuditEntryId,
        val actorUserId: UserId,
        val action: SpaceAuditAction,
        val changeSummary: String,
        val createdAt: Instant
    )
}
