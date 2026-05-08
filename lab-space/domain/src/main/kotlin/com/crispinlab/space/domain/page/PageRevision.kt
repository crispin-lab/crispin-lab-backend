package com.crispinlab.space.domain.page

import com.crispinlab.space.domain.user.UserId
import java.time.Instant

class PageRevision(
    val id: PageRevisionId,
    val pageId: PageId,
    val version: Int,
    val title: String,
    val content: PageContent,
    val authorId: UserId,
    val createdAt: Instant
) {
    init {
        require(version >= 1) {
            "리비전 버전은 1 이상이어야 합니다."
        }
        require(title.isNotBlank()) {
            "제목을 입력해 주세요."
        }
    }
}
