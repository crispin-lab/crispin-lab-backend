package com.crispinlab.space.application.port.outgoing.space

import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.domain.space.SpaceId

interface SpaceRepository {
    fun save(space: Space): Space

    fun findBy(id: SpaceId): Space?

    fun findAll(): List<Space>

    fun delete(id: SpaceId)
}
