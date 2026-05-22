package com.crispinlab.space.application.port.outgoing.space

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility

interface SpaceRepository {
    fun save(entity: Space): Space

    fun findBy(id: SpaceId): Space?

    fun findPage(
        pageRequest: PageRequest,
        visibilities: Set<SpaceVisibility>
    ): PageResult<Space>

    fun delete(id: SpaceId)
}
