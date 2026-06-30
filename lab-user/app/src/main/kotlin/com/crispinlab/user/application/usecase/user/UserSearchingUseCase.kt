package com.crispinlab.user.application.usecase.user

import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.user.application.port.incoming.user.UserSearching
import com.crispinlab.user.application.port.incoming.user.UserSearching.Request
import com.crispinlab.user.application.port.incoming.user.UserSearching.Result
import com.crispinlab.user.application.port.outgoing.user.UserSearchPort
import com.crispinlab.user.application.port.outgoing.user.UserSearchPort.Match
import org.springframework.stereotype.Service

@Service
class UserSearchingUseCase(
    private val userSearchPort: UserSearchPort,
    private val transactionProvider: TransactionProvider
) : UserSearching {
    override fun perform(request: Request): Result =
        transactionProvider.transactional(readOnly = true) {
            userSearchPort.search(query = request.query, size = request.size).toResult()
        }

    private fun List<Match>.toResult(): Result =
        Result(
            items =
                map {
                    Result.Item(userId = it.userId, handle = it.handle)
                }
        )
}
