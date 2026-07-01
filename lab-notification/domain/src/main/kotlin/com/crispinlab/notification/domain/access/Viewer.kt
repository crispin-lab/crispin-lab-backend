package com.crispinlab.notification.domain.access

import com.crispinlab.user.domain.user.UserId

sealed interface Viewer {
    val isAdmin: Boolean
    val isAuthenticated: Boolean

    data object Anonymous : Viewer {
        override val isAdmin: Boolean = false
        override val isAuthenticated: Boolean = false
    }

    data class Member(
        val userId: UserId,
        override val isAdmin: Boolean
    ) : Viewer {
        override val isAuthenticated: Boolean = true
    }
}
