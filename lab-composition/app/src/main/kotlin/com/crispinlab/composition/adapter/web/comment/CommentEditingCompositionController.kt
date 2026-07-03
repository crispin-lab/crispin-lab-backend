package com.crispinlab.composition.adapter.web.comment

import com.crispinlab.composition.application.port.incoming.comment.CommentEditingComposition
import com.crispinlab.composition.application.port.incoming.comment.CommentEditingComposition.Request
import com.crispinlab.composition.application.port.incoming.comment.CommentEditingComposition.Result
import com.crispinlab.space.adapter.web.auth.toMember
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.user.adapter.web.auth.Auth
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/pages/{pageId}/comments/{commentId}")
class CommentEditingCompositionController(
    private val useCase: CommentEditingComposition
) {
    @PutMapping
    fun edit(
        @PathVariable pageId: String,
        @PathVariable commentId: String,
        @RequestBody body: Body,
        auth: Auth
    ): Result =
        body
            .toRequestWith(
                pageId = pageId,
                commentId = commentId,
                viewer = auth.toMember()
            ).let { useCase.perform(it) }

    data class Body(
        val content: String
    ) {
        fun toRequestWith(
            pageId: String,
            commentId: String,
            viewer: Viewer.Member
        ): Request =
            Request(
                pageId = pageId,
                commentId = commentId,
                content = content,
                viewer = viewer
            )
    }
}
