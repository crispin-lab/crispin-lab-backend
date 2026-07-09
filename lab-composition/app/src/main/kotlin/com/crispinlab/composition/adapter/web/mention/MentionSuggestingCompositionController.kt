package com.crispinlab.composition.adapter.web.mention

import com.crispinlab.composition.application.port.incoming.mention.MentionSuggesting
import com.crispinlab.composition.application.port.incoming.mention.MentionSuggesting.Request
import com.crispinlab.composition.application.port.incoming.mention.MentionSuggesting.Result
import com.crispinlab.space.adapter.web.auth.toMember
import com.crispinlab.user.adapter.web.auth.Auth
import com.crispinlab.user.application.port.incoming.user.UserSearching.Request.Companion.DEFAULT_SIZE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/mention-candidates")
class MentionSuggestingCompositionController(
    private val useCase: MentionSuggesting
) {
    @GetMapping
    fun suggest(
        @RequestParam query: String,
        @RequestParam(defaultValue = "$DEFAULT_SIZE") size: Int,
        @RequestParam spaceId: String,
        @RequestParam spaceVisibility: String,
        @RequestParam pageVisibility: String,
        @RequestParam pageAuthorId: String,
        auth: Auth
    ): Result =
        Request(
            query = query,
            size = size,
            spaceId = spaceId,
            spaceVisibility = spaceVisibility,
            pageVisibility = pageVisibility,
            pageAuthorId = pageAuthorId,
            viewer = auth.toMember()
        ).let { useCase.perform(it) }
}
