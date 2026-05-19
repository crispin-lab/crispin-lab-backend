package com.crispinlab.user.application.usecase.auth

import com.crispinlab.common.exception.AuthenticationException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.user.application.port.incoming.auth.AuthLogin
import com.crispinlab.user.application.port.incoming.auth.AuthLogin.Request
import com.crispinlab.user.application.port.incoming.auth.AuthLogin.Result
import com.crispinlab.user.application.port.outgoing.credential.PasswordEncoder
import com.crispinlab.user.application.port.outgoing.credential.UserCredentialRepository
import com.crispinlab.user.application.port.outgoing.session.SessionService
import com.crispinlab.user.application.port.outgoing.user.UserRepository
import com.crispinlab.user.domain.credential.Credential
import com.crispinlab.user.domain.user.EmailAddress
import com.crispinlab.user.domain.user.User
import com.crispinlab.user.domain.user.UserErrorCode
import org.springframework.stereotype.Service

@Service
class AuthLoginUseCase(
    private val userRepository: UserRepository,
    private val userCredentialRepository: UserCredentialRepository,
    private val passwordEncoder: PasswordEncoder,
    private val sessionService: SessionService,
    private val transactionProvider: TransactionProvider
) : AuthLogin {
    override fun perform(request: Request): Result =
        transactionProvider.transactional(readOnly = true) {
            request
                .toEmail()
                .toUser()
                .verifyPassword(request.password)
                .toResult()
        }

    private fun Request.toEmail(): EmailAddress =
        runCatching { EmailAddress(email) }
            .getOrElse { throw invalidCredentials() }

    private fun EmailAddress.toUser(): User =
        userRepository.findByEmail(this)
            ?: throw invalidCredentials()

    private fun User.verifyPassword(rawPassword: String): User =
        apply {
            passwordCredential()
                ?.takeIf { passwordEncoder.matches(rawPassword, it.hash) }
                ?: throw invalidCredentials()
        }

    private fun User.passwordCredential(): Credential.Password? =
        userCredentialRepository.findPasswordBy(id)?.credential as? Credential.Password

    private fun User.toResult(): Result =
        sessionService
            .issue(id)
            .also { token ->
                transactionProvider.afterRollback {
                    sessionService.revoke(token)
                }
            }.let {
                Result(userId = id, token = it)
            }

    private fun invalidCredentials(): AuthenticationException =
        AuthenticationException(UserErrorCode.INVALID_CREDENTIALS)
}
