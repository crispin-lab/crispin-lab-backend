package com.crispinlab.user.application.usecase.user

import com.crispinlab.common.exception.ConflictException
import com.crispinlab.common.id.IdGenerator
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.user.application.port.incoming.user.UserSigning
import com.crispinlab.user.application.port.incoming.user.UserSigning.Request
import com.crispinlab.user.application.port.incoming.user.UserSigning.Result
import com.crispinlab.user.application.port.outgoing.credential.PasswordEncoder
import com.crispinlab.user.application.port.outgoing.credential.UserCredentialRepository
import com.crispinlab.user.application.port.outgoing.session.SessionService
import com.crispinlab.user.application.port.outgoing.user.UserRepository
import com.crispinlab.user.domain.credential.Credential
import com.crispinlab.user.domain.credential.UserCredential
import com.crispinlab.user.domain.credential.UserCredentialId
import com.crispinlab.user.domain.user.User
import com.crispinlab.user.domain.user.UserErrorCode
import com.crispinlab.user.domain.user.UserId
import org.springframework.stereotype.Service

@Service
class UserSigningUseCase(
    private val userRepository: UserRepository,
    private val userCredentialRepository: UserCredentialRepository,
    private val passwordEncoder: PasswordEncoder,
    private val sessionService: SessionService,
    private val idGenerator: IdGenerator,
    private val transactionProvider: TransactionProvider
) : UserSigning {
    override fun perform(request: Request): Result =
        transactionProvider.transactional {
            request
                .also {
                    it.validate()
                }.toEntity()
                .save()
                .also {
                    it.saveCredential(request.password)
                }.toResult()
        }

    private fun Request.validate() {
        if (userRepository.existsByEmail(email)) {
            throw ConflictException(UserErrorCode.EMAIL_DUPLICATED)
        }
        if (userRepository.existsByHandle(handle)) {
            throw ConflictException(UserErrorCode.HANDLE_DUPLICATED)
        }
    }

    private fun Request.toEntity(): User =
        User(
            id = UserId(idGenerator.next()),
            email = email,
            handle = handle
        )

    private fun User.save(): User = userRepository.save(this)

    private fun User.saveCredential(rawPassword: String) {
        UserCredential(
            id = UserCredentialId(idGenerator.next()),
            userId = id,
            credential = Credential.Password(passwordEncoder.encode(rawPassword))
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
}
