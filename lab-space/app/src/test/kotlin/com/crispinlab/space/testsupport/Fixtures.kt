package com.crispinlab.space.testsupport

import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT

object Fixtures {
    fun basicSpace(
        id: SpaceId = SpaceId(1L),
        name: String = "테스트 스페이스",
        description: String = "설명"
    ): Space =
        Space(
            id = id,
            name = name,
            description = description,
            createdAt = DUMMY_INSTANT,
            updatedAt = DUMMY_INSTANT
        )
}
