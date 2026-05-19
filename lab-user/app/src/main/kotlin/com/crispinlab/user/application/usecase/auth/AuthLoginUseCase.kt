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
        transactionProvider.transactional {
            request
                .toEntity()
                .toResult()
        }

    private fun Request.toEntity(): User {
        val emailAddress =
            runCatching { EmailAddress(email) }
                .getOrElse { throw invalidCredentials() }
        val user =
            userRepository.findByEmail(emailAddress)
                ?: throw invalidCredentials()
        val passwordCredential =
            userCredentialRepository
                .findPasswordBy(user.id)
                ?.credential as? Credential.Password
                ?: throw invalidCredentials()
        if (!passwordEncoder.matches(password, passwordCredential.hash)) {
            throw invalidCredentials()
        }
        return user
    }

    private fun User.toResult(): Result =
        Result(
            userId = id,
            token = sessionService.issue(id)
        )

    private fun invalidCredentials(): AuthenticationException =
        AuthenticationException(UserErrorCode.INVALID_CREDENTIALS)
}
