package com.crispinlab.composition.adapter.web.page

import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.composition.application.port.outgoing.user.UserHandleLookup
import com.crispinlab.space.adapter.web.auth.toViewer
import com.crispinlab.space.application.port.incoming.page.PageInboundLinkListing
import com.crispinlab.space.application.port.incoming.page.PageInboundLinkListing.Request
import com.crispinlab.space.application.port.incoming.page.PageInboundLinkListing.Summary
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.adapter.web.auth.Auth
import com.crispinlab.user.domain.user.UserId
import java.time.Instant
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/pages/{pageId}/inbound")
class PageInboundLinkListingCompositionController(
    private val useCase: PageInboundLinkListing,
    private val userHandleLookup: UserHandleLookup
) {
    @GetMapping
    fun list(
        @PathVariable pageId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "$DEFAULT_SIZE") size: Int,
        auth: Auth?
    ): PageResult<InboundLinkSummary> =
        Request(
            pageId = pageId,
            page = page,
            size = size,
            viewer = auth.toViewer()
        ).let { useCase.perform(it) }
            .toPayloads()

    private fun PageResult<Summary>.toPayloads(): PageResult<InboundLinkSummary> {
        val handles = userHandleLookup.handlesOf(items.map { it.authorId }.toSet())
        return map { it.toSummary(handles) }
    }

    private fun Summary.toSummary(handles: Map<UserId, String>): InboundLinkSummary =
        InboundLinkSummary(
            pageId = pageId,
            spaceId = spaceId,
            parentPageId = parentPageId,
            authorId = authorId,
            authorHandle = handles[authorId] ?: "",
            title = title,
            visibility = visibility,
            displayOrder = displayOrder,
            updatedAt = updatedAt
        )

    data class InboundLinkSummary(
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
