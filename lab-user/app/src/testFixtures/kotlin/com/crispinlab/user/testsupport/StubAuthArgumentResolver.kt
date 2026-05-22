package com.crispinlab.user.testsupport

import com.crispinlab.common.exception.AuthenticationException
import com.crispinlab.user.adapter.web.auth.Auth
import com.crispinlab.user.domain.session.SessionErrorCode.INVALID_SESSION
import com.crispinlab.user.domain.user.SystemRole
import com.crispinlab.user.domain.user.UserId
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

class StubAuthArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.parameterType == Auth::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Auth {
        val raw = webRequest.getHeader(AUTHORIZATION) ?: throw invalidSession()
        val payload = raw.removePrefix(BEARER_PREFIX)
        if (payload == raw || payload.isBlank()) throw invalidSession()
        val parts = payload.split(":", limit = 2).takeIf { it.size == 2 } ?: throw invalidSession()
        val userId = parts[0].toLongOrNull() ?: throw invalidSession()
        val role =
            runCatching { SystemRole.valueOf(parts[1]) }.getOrNull() ?: throw invalidSession()
        return Auth(userId = UserId(userId), role = role)
    }

    private fun invalidSession(): AuthenticationException = AuthenticationException(INVALID_SESSION)

    companion object {
        private const val BEARER_PREFIX: String = "Bearer "
    }
}

fun MockHttpServletRequestBuilder.withAuth(
    userId: String = "100",
    role: SystemRole = SystemRole.USER
): MockHttpServletRequestBuilder = header(AUTHORIZATION, "Bearer $userId:${role.name}")
