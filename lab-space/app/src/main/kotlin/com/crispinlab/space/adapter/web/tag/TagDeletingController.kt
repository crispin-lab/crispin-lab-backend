package com.crispinlab.space.adapter.web.tag

import com.crispinlab.space.adapter.web.auth.Auth
import com.crispinlab.space.application.port.incoming.tag.TagDeleting
import com.crispinlab.space.application.port.incoming.tag.TagDeleting.Request
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/tags/{tagId}")
class TagDeletingController(
    private val useCase: TagDeleting
) {
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable tagId: String,
        auth: Auth
    ) {
        Request(
            tagId = tagId,
            currentUserId = auth.userId
        ).let {
            useCase.perform(it)
        }
    }
}
