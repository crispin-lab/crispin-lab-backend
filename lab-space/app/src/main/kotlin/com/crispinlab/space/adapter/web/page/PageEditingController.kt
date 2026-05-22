package com.crispinlab.space.adapter.web.page

import com.crispinlab.space.application.port.incoming.page.PageEditing
import com.crispinlab.space.application.port.incoming.page.PageEditing.Request
import com.crispinlab.space.application.port.incoming.page.PageEditing.Result
import com.crispinlab.user.adapter.web.auth.Auth
import com.crispinlab.user.domain.user.AuthContext
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/pages/{pageId}")
class PageEditingController(
    private val useCase: PageEditing
) {
    @PutMapping
    fun edit(
        @PathVariable pageId: String,
        @RequestBody body: Body,
        auth: Auth
    ): Result =
        body
            .toRequestWith(pageId = pageId, auth = auth.toContext())
            .let {
                useCase.perform(it)
            }

    data class Body(
        val title: String,
        val content: String
    ) {
        fun toRequestWith(
            pageId: String,
            auth: AuthContext.Authenticated
        ): Request =
            Request(
                pageId = pageId,
                title = title,
                content = content,
                auth = auth
            )
    }
}
