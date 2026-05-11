package com.crispinlab.space.adapter.web.auth

import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class AuthArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.parameterType == Auth::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Auth =
        webRequest.getHeader(USER_ID_HEADER)?.let { Auth(userId = it) }
            ?: throw IllegalArgumentException("$USER_ID_HEADER 헤더가 필요합니다.")

    companion object {
        const val USER_ID_HEADER: String = "X-User-Id"
    }
}
