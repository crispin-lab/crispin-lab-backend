package com.crispinlab.space.adapter.web.space

import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.incoming.space.SpaceListing
import com.crispinlab.space.application.port.incoming.space.SpaceListing.Request
import com.crispinlab.space.application.port.incoming.space.SpaceListing.Summary
import com.crispinlab.user.adapter.web.auth.Auth
import com.crispinlab.user.adapter.web.auth.toContext
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/spaces")
class SpaceListingController(
    private val useCase: SpaceListing
) {
    @GetMapping
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "$DEFAULT_SIZE") size: Int,
        auth: Auth?
    ): PageResult<Summary> =
        Request(
            page = page,
            size = size,
            auth = auth.toContext()
        ).let { useCase.perform(it) }
}
