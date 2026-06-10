package com.crispinlab.space.adapter.web.tag

import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.adapter.web.auth.toViewer
import com.crispinlab.space.application.port.incoming.tag.TagPopularityListing
import com.crispinlab.space.application.port.incoming.tag.TagPopularityListing.Request
import com.crispinlab.space.application.port.incoming.tag.TagPopularityListing.Request.Companion.DEFAULT_POPULAR_SIZE
import com.crispinlab.space.application.port.incoming.tag.TagPopularityListing.Summary
import com.crispinlab.user.adapter.web.auth.Auth
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/tags/popular")
class TagPopularityListingController(
    private val useCase: TagPopularityListing
) {
    @GetMapping
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "$DEFAULT_POPULAR_SIZE") size: Int,
        auth: Auth?
    ): PageResult<Summary> =
        Request(
            page = page,
            size = size,
            viewer = auth.toViewer()
        ).let { useCase.perform(it) }
}
