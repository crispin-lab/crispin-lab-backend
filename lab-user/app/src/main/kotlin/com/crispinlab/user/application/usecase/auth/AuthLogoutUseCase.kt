package com.crispinlab.user.application.usecase.auth

import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.user.application.port.incoming.auth.AuthLogout
import com.crispinlab.user.application.port.incoming.auth.AuthLogout.Request
import com.crispinlab.user.application.port.outgoing.session.SessionService
import org.springframework.stereotype.Service

@Service
class AuthLogoutUseCase(
    private val sessionService: SessionService,
    private val transactionProvider: TransactionProvider
) : AuthLogout {
    override fun perform(request: Request) {
        transactionProvider.transactional {
            sessionService.revoke(request.token)
        }
    }
}
