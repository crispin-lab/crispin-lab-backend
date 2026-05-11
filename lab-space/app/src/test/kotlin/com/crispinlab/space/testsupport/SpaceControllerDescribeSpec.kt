package com.crispinlab.space.testsupport

import com.crispinlab.apisupport.testsupport.ControllerDescribeSpec
import com.crispinlab.space.adapter.web.GlobalExceptionHandler
import com.crispinlab.space.adapter.web.auth.AuthArgumentResolver

abstract class SpaceControllerDescribeSpec(
    tag: String,
    body: ControllerDescribeSpec.() -> Unit
) : ControllerDescribeSpec(
        tag = tag,
        argumentResolvers = listOf(AuthArgumentResolver()),
        controllerAdvices = listOf(GlobalExceptionHandler()),
        body = body
    )
