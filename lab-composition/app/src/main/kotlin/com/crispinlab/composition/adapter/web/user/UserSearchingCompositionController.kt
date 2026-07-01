package com.crispinlab.composition.adapter.web.user

import com.crispinlab.composition.application.port.outgoing.space.SpaceMembershipLookup
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.adapter.web.auth.Auth
import com.crispinlab.user.application.port.incoming.user.UserSearching
import com.crispinlab.user.application.port.incoming.user.UserSearching.Request
import com.crispinlab.user.application.port.incoming.user.UserSearching.Request.Companion.DEFAULT_SIZE
import com.crispinlab.user.application.port.incoming.user.UserSearching.Result
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.UserId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/users")
class UserSearchingCompositionController(
    private val useCase: UserSearching,
    private val spaceMembershipLookup: SpaceMembershipLookup
) {
    @GetMapping
    fun search(
        @RequestParam query: String,
        @RequestParam(defaultValue = "$DEFAULT_SIZE") size: Int,
        @Suppress("UNUSED_PARAMETER") auth: Auth
    ): UserSearchPayload =
        Request(query = query, size = size)
            .let { useCase.perform(it) }
            .toPayload()

    private fun Result.toPayload(): UserSearchPayload {
        val memberships =
            runCatching {
                spaceMembershipLookup.membershipsOf(items.map { it.userId }.toSet())
            }.getOrElse { emptyMap() }
        return UserSearchPayload(
            items = items.map { it.toItem(memberships) }
        )
    }

    private fun Result.Item.toItem(memberships: Map<UserId, Set<SpaceId>>): UserSearchItem =
        UserSearchItem(
            userId = userId,
            handle = handle,
            memberOfSpaceIds =
                memberships[userId]
                    .orEmpty()
                    .sortedBy { it.value }
        )

    data class UserSearchPayload(
        val items: List<UserSearchItem>
    )

    data class UserSearchItem(
        val userId: UserId,
        val handle: Handle,
        val memberOfSpaceIds: List<SpaceId>
    )
}
