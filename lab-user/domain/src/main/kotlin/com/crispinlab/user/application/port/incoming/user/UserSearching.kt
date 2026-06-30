package com.crispinlab.user.application.port.incoming.user

import com.crispinlab.common.application.UseCase
import com.crispinlab.user.application.port.incoming.user.UserSearching.Request
import com.crispinlab.user.application.port.incoming.user.UserSearching.Result
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.UserId
import com.crispinlab.user.domain.user.UserId.Companion.asUserId

interface UserSearching : UseCase<Request, Result> {
    class Request(
        query: String,
        size: Int = DEFAULT_SIZE,
        currentUserId: String
    ) {
        val query: String =
            query.trim().also {
                require(it.length in MIN_QUERY_LENGTH..MAX_QUERY_LENGTH) {
                    "검색어는 ${MIN_QUERY_LENGTH}자 이상 ${MAX_QUERY_LENGTH}자 이하여야 합니다."
                }
            }
        val size: Int =
            size.also {
                require(it in 1..MAX_SIZE) {
                    "결과 수는 1 이상 ${MAX_SIZE} 이하여야 합니다."
                }
            }
        val currentUserId: UserId = currentUserId.asUserId()

        companion object {
            const val MIN_QUERY_LENGTH: Int = 1
            const val MAX_QUERY_LENGTH: Int = Handle.MAX_LENGTH
            const val DEFAULT_SIZE: Int = 10
            const val MAX_SIZE: Int = 20
        }
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
