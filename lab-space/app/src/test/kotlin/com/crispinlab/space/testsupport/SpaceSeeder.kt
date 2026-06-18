package com.crispinlab.space.testsupport

import com.crispinlab.space.adapter.persistence.space.ExposedSpaceRepository
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.testsupport.Fixtures.basicSpace
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun seedSpaces(
    database: Database,
    vararg spaces: Pair<Long, SpaceVisibility>
) {
    val repository = ExposedSpaceRepository()
    transaction(database) {
        spaces.forEach { (id, visibility) ->
            repository.save(basicSpace(id = SpaceId(id), visibility = visibility))
        }
    }
}

fun seedPublicSpaces(
    database: Database,
    vararg spaceIds: Long
) {
    seedSpaces(database, *spaceIds.map { it to SpaceVisibility.PUBLIC }.toTypedArray())
}
