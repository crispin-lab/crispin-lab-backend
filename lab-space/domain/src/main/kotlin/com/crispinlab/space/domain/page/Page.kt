package com.crispinlab.space.domain.page

import com.crispinlab.common.domain.Entity
import com.crispinlab.common.domain.SoftDeletable
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.domain.user.UserId
import java.time.Instant
import java.time.Instant.now

class Page(
    override val id: PageId,
    val spaceId: SpaceId,
    parentPageId: PageId?,
    val authorId: UserId,
    title: String,
    content: PageContent,
    visibility: Visibility,
    currentVersion: Int,
    displayOrder: Int = 0,
    val createdAt: Instant = now(),
    updatedAt: Instant = createdAt,
    deletedAt: Instant? = null
) : Entity<PageId>,
    SoftDeletable {
    var parentPageId: PageId? = parentPageId
        private set
    var title: String = title
        private set
    var content: PageContent = content
        private set
    var visibility: Visibility = visibility
        private set
    var currentVersion: Int = currentVersion
        private set
    var displayOrder: Int = displayOrder
        private set
    var updatedAt: Instant = updatedAt
        private set
    override var deletedAt: Instant? = deletedAt
        private set

    init {
        validateTitle(title)
        require(currentVersion >= 1) {
            "현재 버전은 1 이상이어야 합니다."
        }
        require(displayOrder >= 0) {
            "표시 순서는 0 이상이어야 합니다."
        }
        require(parentPageId != id) {
            "자기 자신을 부모로 설정할 수 없습니다."
        }
    }

    fun edit(
        title: String,
        content: String
    ): EditResult {
        check(!isDeleted) {
            "삭제된 페이지는 수정할 수 없습니다."
        }
        validateTitle(title)
        val newContent: PageContent = PageContent(content)
        val newVersion: Int = currentVersion + 1
        val occurredAt: Instant = now()

        this.title = title
        this.content = newContent
        this.currentVersion = newVersion
        this.updatedAt = occurredAt

        return EditResult(
            version = newVersion,
            title = title,
            content = newContent,
            occurredAt = occurredAt
        )
    }

    /**
     * 부모 페이지를 옮긴다. 자기 자신을 부모로 두는 케이스만 막는다.
     * 자손 페이지 밑으로의 순환 이동 검증은 repository 조회가 필요하므로 UseCase 책임이다.
     */
    fun move(parentPageId: PageId?) {
        check(!isDeleted) {
            "삭제된 페이지는 이동할 수 없습니다."
        }
        require(parentPageId != id) {
            "자기 자신을 부모로 설정할 수 없습니다."
        }
        this.parentPageId = parentPageId
        this.updatedAt = now()
    }

    fun changeVisibility(visibility: Visibility) {
        check(!isDeleted) {
            "삭제된 페이지의 공개 범위는 변경할 수 없습니다."
        }
        this.visibility = visibility
        this.updatedAt = now()
    }

    private fun validateTitle(title: String) {
        require(title.isNotBlank()) {
            "제목을 입력해 주세요."
        }
        require(title.length <= MAX_TITLE_LENGTH) {
            "제목은 ${MAX_TITLE_LENGTH}자를 넘을 수 없습니다."
        }
    }

    data class EditResult(
        val version: Int,
        val title: String,
        val content: PageContent,
        val occurredAt: Instant
    )

    companion object {
        const val MAX_TITLE_LENGTH: Int = 200
    }
}
