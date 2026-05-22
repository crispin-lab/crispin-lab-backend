package com.crispinlab.space.adapter.web.page

import com.crispinlab.space.adapter.web.auth.toViewer
import com.crispinlab.space.application.port.incoming.page.PageRegistering
import com.crispinlab.space.application.port.incoming.page.PageRegistering.Request
import com.crispinlab.space.application.port.incoming.page.PageRegistering.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.user.adapter.web.auth.Auth
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/pages")
class PageRegisteringController(
    private val useCase: PageRegistering
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @RequestBody body: Body,
        auth: Auth
    ): Result =
        body
            .toRequestWith(auth.toViewer())
            .let {
                useCase.perform(it)
            }

    data class Body(
        val spaceId: String,
        val parentPageId: String? = null,
        val title: String,
        val content: String,
        val visibility: String
    ) {
        fun toRequestWith(viewer: Viewer.Member): Request =
            Request(
                spaceId = spaceId,
                parentPageId = parentPageId,
                title = title,
                content = content,
                visibility = visibility,
                viewer = viewer
            )
    }
}
