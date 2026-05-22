package com.crispinlab.space.adapter.web.comment

import com.crispinlab.space.application.port.incoming.comment.CommentRegistering
import com.crispinlab.space.application.port.incoming.comment.CommentRegistering.Request
import com.crispinlab.space.application.port.incoming.comment.CommentRegistering.Result
import com.crispinlab.user.adapter.web.auth.Auth
import com.crispinlab.user.domain.user.UserId
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/pages/{pageId}/comments")
class CommentRegisteringController(
    private val useCase: CommentRegistering
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @PathVariable pageId: String,
        @RequestBody body: Body,
        auth: Auth
    ): Result =
        body
            .toRequestWith(
                pageId = pageId,
                userId = auth.userId
            ).let {
                useCase.perform(it)
            }

    data class Body(
        val body: String
    ) {
        fun toRequestWith(
            pageId: String,
            userId: UserId
        ): Request =
            Request(
                pageId = pageId,
                body = body,
                currentUserId = userId
            )
    }
}
