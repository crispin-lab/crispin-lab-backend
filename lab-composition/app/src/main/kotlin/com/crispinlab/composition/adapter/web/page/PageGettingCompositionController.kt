package com.crispinlab.composition.adapter.web.page

import com.crispinlab.composition.application.port.incoming.page.PageGettingComposition
import com.crispinlab.composition.application.port.incoming.page.PageGettingComposition.Request
import com.crispinlab.composition.application.port.incoming.page.PageGettingComposition.Result
import com.crispinlab.space.adapter.web.auth.toViewer
import com.crispinlab.user.adapter.web.auth.Auth
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/pages/{pageId}")
class PageGettingCompositionController(
    private val useCase: PageGettingComposition
) {
    @GetMapping
    fun get(
        @PathVariable pageId: String,
        auth: Auth?
    ): Result =
        Request(
            pageId = pageId,
            viewer = auth.toViewer()
        ).let { useCase.perform(it) }
}
