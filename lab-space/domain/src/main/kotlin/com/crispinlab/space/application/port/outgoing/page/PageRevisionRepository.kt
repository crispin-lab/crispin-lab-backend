package com.crispinlab.space.application.port.outgoing.page

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageRevision
import com.crispinlab.space.domain.page.PageRevisionId

interface PageRevisionRepository {
    fun save(entity: PageRevision): PageRevision

    fun findBy(id: PageRevisionId): PageRevision?

    fun findBy(
        pageId: PageId,
        version: Int
    ): PageRevision?

    fun findByPageId(
        pageId: PageId,
        pageRequest: PageRequest
    ): PageResult<PageRevision>
}
