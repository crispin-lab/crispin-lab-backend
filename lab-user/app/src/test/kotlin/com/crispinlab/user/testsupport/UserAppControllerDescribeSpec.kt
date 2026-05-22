package com.crispinlab.user.testsupport

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec
import com.crispinlab.common.infra.web.GlobalExceptionHandler

abstract class UserAppControllerDescribeSpec(
    tag: String,
    body: ControllerDescribeSpec.() -> Unit
) : ControllerDescribeSpec(
        tag = tag,
        argumentResolvers = listOf(StubAuthArgumentResolver()),
        controllerAdvices = listOf(GlobalExceptionHandler()),
        body = body
    )
