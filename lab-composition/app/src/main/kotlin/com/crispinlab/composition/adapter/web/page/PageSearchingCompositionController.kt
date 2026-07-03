package com.crispinlab.composition.adapter.web.page

import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.composition.application.port.incoming.page.PageSearchingComposition
import com.crispinlab.composition.application.port.incoming.page.PageSearchingComposition.Request
import com.crispinlab.composition.application.port.incoming.page.PageSearchingComposition.Result
import com.crispinlab.space.adapter.web.auth.toViewer
import com.crispinlab.user.adapter.web.auth.Auth
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/pages")
class PageSearchingCompositionController(
    private val useCase: PageSearchingComposition
) {
    @GetMapping
    fun search(
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) space: String?,
        @RequestParam(required = false) tag: List<String>?,
        @RequestParam(required = false) tagName: String?,
        @RequestParam(required = false) sort: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "$DEFAULT_SIZE") size: Int,
        auth: Auth?
    ): PageResult<Result> =
        Request(
            keyword = query,
            spaceId = space,
            tagIds = tag.orEmpty(),
            tagName = tagName,
            sort = sort,
            page = page,
            size = size,
            viewer = auth.toViewer()
        ).let { useCase.perform(it) }
}
