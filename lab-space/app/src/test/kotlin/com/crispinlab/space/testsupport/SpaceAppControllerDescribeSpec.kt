package com.crispinlab.space.testsupport

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec
import com.crispinlab.common.infra.web.GlobalExceptionHandler
import com.crispinlab.user.testsupport.StubAuthArgumentResolver

abstract class SpaceAppControllerDescribeSpec(
    tag: String,
    body: ControllerDescribeSpec.() -> Unit
) : ControllerDescribeSpec(
        tag = tag,
        argumentResolvers = listOf(StubAuthArgumentResolver()),
        controllerAdvices = listOf(GlobalExceptionHandler()),
        body = body
    )
