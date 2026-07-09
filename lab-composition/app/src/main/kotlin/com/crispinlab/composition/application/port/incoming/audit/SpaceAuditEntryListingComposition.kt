package com.crispinlab.composition.application.port.incoming.audit

import com.crispinlab.common.application.UseCase
import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.composition.application.port.incoming.audit.SpaceAuditEntryListingComposition.Request
import com.crispinlab.composition.application.port.incoming.audit.SpaceAuditEntryListingComposition.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.audit.SpaceAuditAction
import com.crispinlab.space.domain.audit.SpaceAuditEntryId
import com.crispinlab.user.domain.user.UserId
import java.time.Instant

interface SpaceAuditEntryListingComposition : UseCase<Request, PageResult<Result>> {
    class Request(
        val spaceId: String,
        val page: Int = 0,
        val size: Int = PageRequest.DEFAULT_SIZE,
        val viewer: Viewer.Member
    )

    data class Result(
        val id: SpaceAuditEntryId,
        val actorUserId: UserId,
        val actorHandle: String,
        val action: SpaceAuditAction,
        val changeSummary: String,
        val createdAt: Instant
    )
}
