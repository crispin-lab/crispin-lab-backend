package com.crispinlab.user.application.usecase.user

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.user.application.port.incoming.user.UserMeRetrieving
import com.crispinlab.user.application.port.incoming.user.UserMeRetrieving.Request
import com.crispinlab.user.application.port.incoming.user.UserMeRetrieving.Result
import com.crispinlab.user.application.port.outgoing.user.UserRepository
import com.crispinlab.user.domain.user.SystemRole
import com.crispinlab.user.domain.user.User
import com.crispinlab.user.domain.user.UserErrorCode
import org.springframework.stereotype.Service

@Service
class UserMeRetrievingUseCase(
    private val userRepository: UserRepository,
    private val transactionProvider: TransactionProvider
) : UserMeRetrieving {
    override fun perform(request: Request): Result =
        transactionProvider.transactional(readOnly = true) {
            request
                .also { it.validate() }
                .toEntity()
                .toResult()
        }

    private fun Request.validate() {
        /*
        todo    :: 권한·존재 등 외부 의존 검증을 둘 자리. 비어 있어도 perform 흐름 정렬을 위해 유지.
         author :: heechoel shin
         date   :: 2026-06-04T16:22:01KST
         ticket :: LAB-86
         */
    }

    private fun Request.toEntity(): User =
        userRepository.findBy(currentUserId)
            ?: throw NotFoundException(UserErrorCode.USER_NOT_FOUND)

    private fun User.toResult(): Result =
        Result(
            userId = id,
            handle = handle,
            email = email,
            isAdmin = role == SystemRole.ADMIN
        )
}
