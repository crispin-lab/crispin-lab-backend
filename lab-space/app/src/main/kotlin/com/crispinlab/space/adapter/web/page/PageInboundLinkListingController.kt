package com.crispinlab.space.adapter.web.page

import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.adapter.web.auth.toViewer
import com.crispinlab.space.application.port.incoming.page.PageInboundLinkListing
import com.crispinlab.space.application.port.incoming.page.PageInboundLinkListing.Request
import com.crispinlab.space.application.port.incoming.page.PageInboundLinkListing.Summary
import com.crispinlab.user.adapter.web.auth.Auth
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/pages/{pageId}/inbound")
class PageInboundLinkListingController(
    private val useCase: PageInboundLinkListing
) {
    @GetMapping
    fun list(
        @PathVariable pageId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "$DEFAULT_SIZE") size: Int,
        auth: Auth?
    ): PageResult<Summary> =
        Request(
            pageId = pageId,
            page = page,
            size = size,
            viewer = auth.toViewer()
        ).let {
            useCase.perform(it)
        }
}
