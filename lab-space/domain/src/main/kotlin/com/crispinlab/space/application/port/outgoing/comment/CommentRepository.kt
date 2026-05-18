package com.crispinlab.space.application.port.outgoing.comment

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.domain.comment.Comment
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.domain.page.PageId

interface CommentRepository {
    fun save(entity: Comment): Comment

    fun findBy(id: CommentId): Comment?

    fun findByPageId(
        pageId: PageId,
        pageRequest: PageRequest
    ): PageResult<Comment>

    /**
     * 물리적 삭제 (관리자 영구 제거 등) 전용.
     * 일반 사용자 흐름의 삭제는 `Comment.delete()` 로 deletedAt 을 설정한 뒤 `save(comment)` 를 사용한다.
     */
    fun delete(id: CommentId)
}
