package com.crispinlab.space.adapter.web.tag

import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.incoming.tag.TagListing
import com.crispinlab.space.application.port.incoming.tag.TagListing.Request
import com.crispinlab.space.application.port.incoming.tag.TagListing.Summary
import com.crispinlab.user.adapter.web.auth.Auth
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/spaces/{spaceId}/tags")
class TagListingController(
    private val useCase: TagListing
) {
    @GetMapping
    fun list(
        @PathVariable spaceId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "$DEFAULT_SIZE") size: Int,
        auth: Auth
    ): PageResult<Summary> =
        Request(
            spaceId = spaceId,
            page = page,
            size = size,
            currentUserId = auth.userId
        ).let {
            useCase.perform(it)
        }
}
