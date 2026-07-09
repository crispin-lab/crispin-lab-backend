package com.crispinlab.composition.application.port.incoming.mention

import com.crispinlab.common.application.UseCase
import com.crispinlab.composition.application.port.incoming.mention.MentionSuggesting.Request
import com.crispinlab.composition.application.port.incoming.mention.MentionSuggesting.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.page.Visibility.Companion.asVisibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceId.Companion.asSpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.domain.space.SpaceVisibility.Companion.asSpaceVisibility
import com.crispinlab.user.application.port.incoming.user.UserSearching
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.UserId
import com.crispinlab.user.domain.user.UserId.Companion.asUserId

interface MentionSuggesting : UseCase<Request, Result> {
    class Request(
        val query: String,
        size: Int,
        spaceId: String,
        spaceVisibility: String,
        pageVisibility: String,
        pageAuthorId: String,
        val viewer: Viewer.Member
    ) {
        val size: Int =
            size.also {
                require(it in 1..UserSearching.Request.MAX_SIZE) {
                    "결과 수는 1 이상 ${UserSearching.Request.MAX_SIZE} 이하여야 합니다."
                }
            }
        val spaceId: SpaceId = spaceId.asSpaceId()
        val spaceVisibility: SpaceVisibility = spaceVisibility.asSpaceVisibility()
        val pageVisibility: Visibility = pageVisibility.asVisibility()
        val pageAuthorId: UserId = pageAuthorId.asUserId()
    }

    data class Result(
        val items: List<Item>
    ) {
        data class Item(
            val userId: UserId,
            val handle: Handle
        )
    }
}
