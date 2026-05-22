package com.crispinlab.app.config

import com.crispinlab.common.logging.LogContext.Field
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.user.application.port.outgoing.user.UserRepository
import com.crispinlab.user.domain.user.EmailAddress
import com.crispinlab.user.domain.user.SystemRole
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class AdminBootstrapApplicationRunner(
    @param:Value("\${admin.email:}") private val adminEmail: String,
    private val userRepository: UserRepository,
    private val transactionProvider: TransactionProvider
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        if (adminEmail.isBlank()) return
        val email =
            runCatching { EmailAddress(adminEmail) }
                .getOrElse {
                    log.warn(
                        "ADMIN 부트스트랩 건너뜀 {}={}",
                        Field.REASON,
                        "invalid_email_format"
                    )
                    return
                }
        transactionProvider.transactional {
            userRepository
                .findByEmail(email)
                ?.takeIf { it.role != SystemRole.ADMIN }
                ?.also {
                    it.promoteTo(SystemRole.ADMIN)
                    userRepository.save(it)
                    log.info("ADMIN 부트스트랩 완료 {}={}", Field.USER_ID, it.id.value)
                }
        }
    }
}
