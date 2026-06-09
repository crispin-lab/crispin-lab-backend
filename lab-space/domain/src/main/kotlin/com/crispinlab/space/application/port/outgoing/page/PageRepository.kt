package com.crispinlab.space.application.port.outgoing.page

import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.space.SpaceId

interface PageRepository {
    fun save(entity: Page): Page

    fun findBy(id: PageId): Page?

    fun findChildren(parentId: PageId): List<Page>

    fun findRoots(spaceId: SpaceId): List<Page>

    fun nextDisplayOrderIn(
        spaceId: SpaceId,
        parentPageId: PageId?
    ): Int

    fun delete(id: PageId)
}
