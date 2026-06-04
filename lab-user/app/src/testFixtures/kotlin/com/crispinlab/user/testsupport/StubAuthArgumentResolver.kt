package com.crispinlab.user.testsupport

import com.crispinlab.common.exception.AuthenticationException
import com.crispinlab.user.adapter.web.auth.Auth
import com.crispinlab.user.domain.session.SessionErrorCode.INVALID_SESSION
import com.crispinlab.user.domain.session.SessionToken
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
    ): Auth? {
        val raw = webRequest.getHeader(AUTHORIZATION)
        if (raw == null) {
            return if (parameter.isOptional) null else throw invalidSession()
        }
        return resolveAuth(raw)
    }

    private fun resolveAuth(raw: String): Auth {
        if (!raw.regionMatches(0, BEARER_PREFIX, 0, BEARER_PREFIX.length, ignoreCase = true)) {
            throw invalidSession()
        }
        val payload = raw.substring(BEARER_PREFIX.length).trim()
        if (payload.isEmpty()) throw invalidSession()
        val parts = payload.split(":", limit = 3)
        if (parts.size < 2) throw invalidSession()
        val userId = parts[0].toLongOrNull() ?: throw invalidSession()
        val role =
            runCatching { SystemRole.valueOf(parts[1]) }.getOrNull() ?: throw invalidSession()
        val sessionToken =
            runCatching {
                parts.getOrNull(2)?.let { SessionToken(it) } ?: DEFAULT_SESSION_TOKEN
            }.getOrElse { throw invalidSession() }
        return Auth(
            userId = UserId(userId),
            role = role,
            sessionToken = sessionToken
        )
    }

    private fun invalidSession(): AuthenticationException = AuthenticationException(INVALID_SESSION)

    companion object {
        private const val BEARER_PREFIX: String = "Bearer "
        private val DEFAULT_SESSION_TOKEN: SessionToken =
            SessionToken(SessionToken.PREFIX + "stub_default_" + "0".repeat(30))
    }
}

fun MockHttpServletRequestBuilder.withAuth(
    userId: String = "100",
    role: SystemRole = SystemRole.USER,
    sessionToken: SessionToken? = null
): MockHttpServletRequestBuilder {
    val suffix = sessionToken?.let { ":${it.value}" }.orEmpty()
    return header(AUTHORIZATION, "Bearer $userId:${role.name}$suffix")
}
