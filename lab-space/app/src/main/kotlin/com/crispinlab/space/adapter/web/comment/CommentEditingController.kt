package com.crispinlab.space.adapter.web.comment

import com.crispinlab.space.adapter.web.auth.toMember
import com.crispinlab.space.application.port.incoming.comment.CommentEditing
import com.crispinlab.space.application.port.incoming.comment.CommentEditing.Request
import com.crispinlab.space.application.port.incoming.comment.CommentEditing.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.user.adapter.web.auth.Auth
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/pages/{pageId}/comments/{commentId}")
class CommentEditingController(
    private val useCase: CommentEditing
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
            ).let {
                useCase.perform(it)
            }

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
