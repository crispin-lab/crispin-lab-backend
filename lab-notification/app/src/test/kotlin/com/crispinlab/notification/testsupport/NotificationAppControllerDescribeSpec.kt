package com.crispinlab.notification.testsupport

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec
import com.crispinlab.common.infra.web.GlobalExceptionHandler
import com.crispinlab.user.testsupport.StubAuthArgumentResolver

abstract class NotificationAppControllerDescribeSpec(
    tag: String,
    body: ControllerDescribeSpec.() -> Unit
) : ControllerDescribeSpec(
        tag = tag,
        argumentResolvers = listOf(StubAuthArgumentResolver()),
        controllerAdvices = listOf(GlobalExceptionHandler()),
        body = body
    )
