package com.crispinlab.space.domain.page

import com.crispinlab.common.domain.Entity
import java.net.URI
import java.time.Instant

data class PageLink(
    override val id: PageLinkId,
    val pageId: PageId,
    val revisionId: PageRevisionId,
    val target: Target,
    val createdAt: Instant
) : Entity<PageLinkId> {
    sealed interface Target {
        data class Internal(
            val targetPageId: PageId
        ) : Target

        data class External(
            val url: URI
        ) : Target {
            init {
                require(url.toString().length <= MAX_EXTERNAL_URL_LENGTH) {
                    "외부 링크 URL 은 ${MAX_EXTERNAL_URL_LENGTH}자를 넘을 수 없습니다."
                }
            }
        }
    }

    companion object {
        const val MAX_EXTERNAL_URL_LENGTH: Int = 500
    }
}
