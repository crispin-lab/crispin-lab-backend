package com.crispinlab.space.application.usecase.space

import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceVisibility

internal fun Viewer.allowedSpaceVisibilities(): Set<SpaceVisibility> =
    when (this) {
        is Viewer.Anonymous -> setOf(SpaceVisibility.PUBLIC)
        is Viewer.Member -> setOf(SpaceVisibility.PUBLIC, SpaceVisibility.INTERNAL)
    }
