package com.crispinlab.space.domain.space

data class SpaceSnapshot(
    val name: String,
    val description: String,
    val visibility: SpaceVisibility
) {
    companion object {
        fun of(space: Space): SpaceSnapshot =
            SpaceSnapshot(
                name = space.name,
                description = space.description,
                visibility = space.visibility
            )
    }
}
