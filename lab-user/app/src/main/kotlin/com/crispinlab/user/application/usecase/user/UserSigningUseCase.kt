package com.crispinlab.user.application.usecase.user

import com.crispinlab.common.exception.ConflictException
import com.crispinlab.common.id.IdGenerator
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.user.application.credential.PasswordPolicyException
import com.crispinlab.user.application.port.incoming.user.UserSigning
import com.crispinlab.user.application.port.incoming.user.UserSigning.Request
import com.crispinlab.user.application.port.incoming.user.UserSigning.Result
import com.crispinlab.user.application.port.outgoing.credential.PasswordBlocklistPort
import com.crispinlab.user.application.port.outgoing.credential.PasswordEncoder
import com.crispinlab.user.application.port.outgoing.credential.UserCredentialRepository
import com.crispinlab.user.application.port.outgoing.session.SessionService
import com.crispinlab.user.application.port.outgoing.user.UserRepository
import com.crispinlab.user.domain.credential.Credential
import com.crispinlab.user.domain.credential.Password
import com.crispinlab.user.domain.credential.PasswordErrorCode
import com.crispinlab.user.domain.credential.UserCredential
import com.crispinlab.user.domain.credential.UserCredentialId
import com.crispinlab.user.domain.user.EmailAddress
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.User
import com.crispinlab.user.domain.user.UserErrorCode
import com.crispinlab.user.domain.user.UserId
import org.springframework.stereotype.Service

@Service
class UserSigningUseCase(
    private val userRepository: UserRepository,
    private val userCredentialRepository: UserCredentialRepository,
    private val passwordEncoder: PasswordEncoder,
    private val passwordBlocklist: PasswordBlocklistPort,
    private val sessionService: SessionService,
    private val idGenerator: IdGenerator,
    private val transactionProvider: TransactionProvider
) : UserSigning {
    override fun perform(request: Request): Result {
        val password =
            when (val outcome = request.password) {
                is Password.Outcome.Ok -> outcome.password
                is Password.Outcome.Violation -> throw PasswordPolicyException(outcome.errorCode)
            }
        request.preflightWith(password)
        return transactionProvider.transactional {
            request
                .also { it.validateDuplicates() }
                .toEntity()
                .save()
                .also { it.saveCredential(password) }
                .toResult()
        }
    }

    private fun Request.preflightWith(password: Password) {
        if (password.containsIdentityOf(email, handle)) {
            throw PasswordPolicyException(PasswordErrorCode.PASSWORD_SIMILAR_TO_IDENTITY)
        }
        if (passwordBlocklist.isBlocked(password)) {
            throw PasswordPolicyException(PasswordErrorCode.PASSWORD_BLOCKED)
        }
    }

    private fun Request.validateDuplicates() {
        if (userRepository.existsByEmail(email)) {
            throw ConflictException(UserErrorCode.EMAIL_DUPLICATED)
        }
        if (userRepository.existsByHandle(handle)) {
            throw ConflictException(UserErrorCode.HANDLE_DUPLICATED)
        }
    }

    private fun Password.containsIdentityOf(
        email: EmailAddress,
        handle: Handle
    ): Boolean {
        val lowered = raw.lowercase()
        val emailLocal = email.value.substringBefore('@').lowercase()
        val handleLowered = handle.value.lowercase()
        return (emailLocal.length >= MIN_SIMILARITY_LENGTH && lowered.contains(emailLocal)) ||
            (handleLowered.length >= MIN_SIMILARITY_LENGTH && lowered.contains(handleLowered))
    }

    private fun Request.toEntity(): User =
        User(
            id = UserId(idGenerator.next()),
            email = email,
            handle = handle
        )

    private fun User.save(): User = userRepository.save(this)

    private fun User.saveCredential(password: Password) {
        UserCredential(
            id = UserCredentialId(idGenerator.next()),
            userId = id,
            credential = Credential.Password(passwordEncoder.encode(password))
        ).let {
            userCredentialRepository.save(it)
        }
    }

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

    companion object {
        private const val MIN_SIMILARITY_LENGTH: Int = 4
    }
}
