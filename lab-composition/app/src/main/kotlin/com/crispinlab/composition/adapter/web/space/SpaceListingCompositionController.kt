package com.crispinlab.composition.adapter.web.space

import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.composition.application.port.incoming.space.SpaceListingComposition
import com.crispinlab.composition.application.port.incoming.space.SpaceListingComposition.Request
import com.crispinlab.composition.application.port.incoming.space.SpaceListingComposition.Result
import com.crispinlab.space.adapter.web.auth.toViewer
import com.crispinlab.user.adapter.web.auth.Auth
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/spaces")
class SpaceListingCompositionController(
    private val useCase: SpaceListingComposition
) {
    @GetMapping
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "$DEFAULT_SIZE") size: Int,
        auth: Auth?
    ): PageResult<Result> =
        Request(
            page = page,
            size = size,
            viewer = auth.toViewer()
        ).let { useCase.perform(it) }
}
