package com.crispinlab.space.adapter.web.comment

import com.crispinlab.space.adapter.web.auth.Auth
import com.crispinlab.space.application.port.incoming.comment.CommentDeleting
import com.crispinlab.space.application.port.incoming.comment.CommentDeleting.Request
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/pages/{pageId}/comments/{commentId}")
class CommentDeletingController(
    private val useCase: CommentDeleting
) {
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable pageId: String,
        @PathVariable commentId: String,
        auth: Auth
    ) {
        Request(
            pageId = pageId,
            commentId = commentId,
            currentUserId = auth.userId
        ).let {
            useCase.perform(it)
        }
    }
}
