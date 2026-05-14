package com.crispinlab.space.application.port.outgoing.space

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.domain.space.SpaceId

interface SpaceRepository {
    fun save(space: Space): Space

    fun findBy(id: SpaceId): Space?

    fun findPage(pageRequest: PageRequest): PageResult<Space>

    fun delete(id: SpaceId)
}
