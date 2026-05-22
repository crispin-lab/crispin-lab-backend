package com.crispinlab.space.adapter.web.auth

import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.user.adapter.web.auth.Auth

fun Auth.toViewer(): Viewer.Member = Viewer.Member(userId = userId, isAdmin = isAdmin)

fun Auth?.toViewer(): Viewer = this?.toViewer() ?: Viewer.Anonymous
