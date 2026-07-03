package com.crispinlab.composition.adapter.web.comment

import com.crispinlab.composition.application.port.incoming.comment.CommentGettingComposition
import com.crispinlab.composition.application.port.incoming.comment.CommentGettingComposition.Request
import com.crispinlab.composition.application.port.incoming.comment.CommentGettingComposition.Result
import com.crispinlab.space.adapter.web.auth.toMember
import com.crispinlab.user.adapter.web.auth.Auth
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/pages/{pageId}/comments/{commentId}")
class CommentGettingCompositionController(
    private val useCase: CommentGettingComposition
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
        ).let { useCase.perform(it) }
}
