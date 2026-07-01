package com.crispinlab.notification.adapter.web.auth

import com.crispinlab.notification.domain.access.Viewer
import com.crispinlab.user.adapter.web.auth.Auth

fun Auth.toMember(): Viewer.Member = Viewer.Member(userId = userId, isAdmin = isAdmin)

fun Auth?.toViewer(): Viewer = this?.toMember() ?: Viewer.Anonymous
