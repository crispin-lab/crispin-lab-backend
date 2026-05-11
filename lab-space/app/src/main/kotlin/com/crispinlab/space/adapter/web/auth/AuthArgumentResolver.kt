package com.crispinlab.space.adapter.web.auth

import com.crispinlab.space.domain.user.UserId
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
    ): Auth {
        val raw: String =
            webRequest.getHeader(USER_ID_HEADER)
                ?: throw IllegalArgumentException("사용자 인증이 필요합니다.")
        val userId: Long =
            raw.toLongOrNull()
                ?: throw IllegalArgumentException("사용자 인증이 필요합니다.")
        return Auth(userId = UserId(userId))
    }

    companion object {
        const val USER_ID_HEADER: String = "X-User-Id"
    }
}
