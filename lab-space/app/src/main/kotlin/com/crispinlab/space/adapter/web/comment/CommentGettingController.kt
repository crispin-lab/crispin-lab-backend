package com.crispinlab.space.adapter.web.comment

import com.crispinlab.space.adapter.web.auth.toMember
import com.crispinlab.space.application.port.incoming.comment.CommentGetting
import com.crispinlab.space.application.port.incoming.comment.CommentGetting.Request
import com.crispinlab.space.application.port.incoming.comment.CommentGetting.Result
import com.crispinlab.user.adapter.web.auth.Auth
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/pages/{pageId}/comments/{commentId}")
class CommentGettingController(
    private val useCase: CommentGetting
) {
    @GetMapping
    fun get(
        @PathVariable pageId: String,
        @PathVariable commentId: String,
        auth: Auth
    ): Result =
        Request(
            pageId = pageId,
            commentId = commentId,
            viewer = auth.toMember()
        ).let {
            useCase.perform(it)
        }
}
