package com.crispinlab.space.adapter.web.tag

import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.adapter.web.auth.Auth
import com.crispinlab.space.application.port.incoming.tag.PageTagListing
import com.crispinlab.space.application.port.incoming.tag.PageTagListing.Request
import com.crispinlab.space.application.port.incoming.tag.PageTagListing.Summary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/pages/{pageId}/tags")
class PageTagListingController(
    private val useCase: PageTagListing
) {
    @GetMapping
    fun list(
        @PathVariable pageId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "$DEFAULT_SIZE") size: Int,
        auth: Auth
    ): PageResult<Summary> =
        Request(
            pageId = pageId,
            page = page,
            size = size,
            currentUserId = auth.userId
        ).let {
            useCase.perform(it)
        }
}
