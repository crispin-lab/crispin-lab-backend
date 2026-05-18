package com.crispinlab.space.application.port.outgoing.page

import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageRevision
import com.crispinlab.space.domain.page.PageRevisionId

interface PageRevisionRepository {
    fun save(entity: PageRevision): PageRevision

    fun findBy(id: PageRevisionId): PageRevision?

    fun findByPageId(pageId: PageId): List<PageRevision>

    fun findLatestByPageId(pageId: PageId): PageRevision?
}
