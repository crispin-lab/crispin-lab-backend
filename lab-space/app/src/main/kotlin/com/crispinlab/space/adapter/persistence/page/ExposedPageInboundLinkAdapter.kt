package com.crispinlab.space.adapter.persistence.page

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.adapter.persistence.space.Spaces
import com.crispinlab.space.adapter.persistence.toPageResult
import com.crispinlab.space.adapter.persistence.visibility.toClauses
import com.crispinlab.space.adapter.persistence.visibility.toExposedOp
import com.crispinlab.space.application.port.outgoing.page.PageInboundLinkPort
import com.crispinlab.space.application.port.outgoing.page.PageInboundLinkPort.InboundLinkSummary
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.domain.user.UserId
import org.jetbrains.exposed.v1.core.Exists
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

@Repository
class ExposedPageInboundLinkAdapter : PageInboundLinkPort {
    override fun findInboundLinksOf(
        targetPageId: PageId,
        scope: VisibilityScope,
        pageRequest: PageRequest
    ): PageResult<InboundLinkSummary> =
        Pages
            .join(
                otherTable = Spaces,
                joinType = JoinType.INNER,
                additionalConstraint = { Pages.spaceId eq Spaces.id }
            ).selectAll()
            .where {
                Pages.notDeleted() and
                    Spaces.deletedAt.isNull() and
                    scope.toClauses().toExposedOp() and
                    hasCurrentRevisionLinkTo(targetPageId)
            }.toPageResult(
                pageRequest,
                Pages.updatedAt to SortOrder.DESC,
                Pages.id to SortOrder.DESC
            ) { it.toSummary() }

    private fun hasCurrentRevisionLinkTo(targetPageId: PageId): Op<Boolean> =
        Exists(
            PageLinks
                .innerJoin(
                    otherTable = PageRevisions,
                    onColumn = { revisionId },
                    otherColumn = { id }
                ).select(PageLinks.id)
                .where {
                    (PageLinks.targetPageId eq targetPageId.value) and
                        (PageRevisions.pageId eq Pages.id) and
                        (PageRevisions.version eq Pages.currentVersion)
                }
        )

    private fun ResultRow.toSummary(): InboundLinkSummary =
        InboundLinkSummary(
            pageId = PageId(this[Pages.id]),
            spaceId = SpaceId(this[Pages.spaceId]),
            parentPageId = this[Pages.parentPageId]?.let(::PageId),
            authorId = UserId(this[Pages.authorId]),
            title = this[Pages.title],
            visibility = decodeVisibility(this[Pages.visibility]),
            displayOrder = this[Pages.displayOrder],
            updatedAt = this[Pages.updatedAt]
        )
}
