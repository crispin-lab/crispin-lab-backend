package com.crispinlab.user.adapter.web.auth

import com.crispinlab.common.exception.AuthenticationException
import com.crispinlab.common.logging.LogContext.Field
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.user.application.port.outgoing.session.SessionService
import com.crispinlab.user.application.port.outgoing.user.UserRepository
import com.crispinlab.user.domain.session.SessionErrorCode.INVALID_SESSION
import com.crispinlab.user.domain.session.SessionToken
import org.slf4j.LoggerFactory
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class AuthArgumentResolver(
    private val sessionService: SessionService,
    private val userRepository: UserRepository,
    private val transactionProvider: TransactionProvider
) : HandlerMethodArgumentResolver {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.parameterType == Auth::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Auth? {
        val header = webRequest.getHeader(AUTHORIZATION)
        if (header == null) {
            return if (parameter.isOptional) null else throw invalidSession("missing_header")
        }
        return resolveAuth(header)
    }

    private fun resolveAuth(header: String): Auth {
        if (!header.regionMatches(0, BEARER_PREFIX, 0, BEARER_PREFIX.length, ignoreCase = true)) {
            throw invalidSession("missing_bearer_prefix")
        }
        val stripped = header.substring(BEARER_PREFIX.length).trim()
        if (stripped.isEmpty()) throw invalidSession("empty_token")
        val token =
            runCatching { SessionToken(stripped) }
                .getOrElse { throw invalidSession("bad_token_format", cause = it) }
        val userId = sessionService.find(token) ?: throw invalidSession("session_miss")
        val user =
            transactionProvider.transactional(readOnly = true) {
                userRepository.findBy(userId) ?: throw invalidSession("user_miss")
            }
        return Auth(
            userId = user.id,
            role = user.role,
            sessionToken = token
        )
    }

    private fun invalidSession(
        reason: String,
        cause: Throwable? = null
    ): AuthenticationException {
        log.debug("인증 실패 {}={}", Field.REASON, reason)
        return AuthenticationException(INVALID_SESSION, cause = cause)
    }

    companion object {
        private const val BEARER_PREFIX: String = "Bearer "
    }
}
