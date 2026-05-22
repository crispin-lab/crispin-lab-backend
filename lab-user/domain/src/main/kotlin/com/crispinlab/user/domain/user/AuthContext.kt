package com.crispinlab.user.domain.user

sealed interface AuthContext {
    val isAdmin: Boolean
    val isAuthenticated: Boolean

    data object Anonymous : AuthContext {
        override val isAdmin: Boolean = false
        override val isAuthenticated: Boolean = false
    }

    data class Authenticated(
        val userId: UserId,
        val role: SystemRole
    ) : AuthContext {
        override val isAdmin: Boolean get() = role == SystemRole.ADMIN
        override val isAuthenticated: Boolean = true
    }
}
