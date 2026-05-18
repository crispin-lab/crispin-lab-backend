package com.crispinlab.space.application.port.outgoing.tag

import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.tag.PageTag
import com.crispinlab.space.domain.tag.Tag
import com.crispinlab.space.domain.tag.TagId

interface TagRepository {
    fun save(entity: Tag): Tag

    fun findBy(id: TagId): Tag?

    fun findBySpaceId(spaceId: SpaceId): List<Tag>

    fun existsByNameAndSpaceId(
        spaceId: SpaceId,
        name: String
    ): Boolean

    fun delete(id: TagId)

    fun attach(pageTag: PageTag)

    fun detach(
        pageId: PageId,
        tagId: TagId
    )

    fun findTagsByPageId(pageId: PageId): List<Tag>

    fun findPageIdsByTagId(tagId: TagId): List<PageId>
}
