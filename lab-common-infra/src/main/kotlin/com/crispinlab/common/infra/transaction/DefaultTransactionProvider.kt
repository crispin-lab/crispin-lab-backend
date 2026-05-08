package com.crispinlab.common.infra.transaction

import com.crispinlab.common.transaction.TransactionProvider
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

class DefaultTransactionProvider(
    transactionManager: PlatformTransactionManager
) : TransactionProvider {
    private val readWriteTemplate = TransactionTemplate(transactionManager)
    private val readOnlyTemplate =
        TransactionTemplate(transactionManager).apply {
            isReadOnly = true
        }

    override fun <T> transactional(
        readOnly: Boolean,
        block: () -> T
    ): T =
        requireNotNull(
            templateFor(readOnly).execute {
                block()
            }
        ) {
            "TransactionTemplate.execute가 예기치 않게 null 을 반환했습니다."
        }

    private fun templateFor(readOnly: Boolean): TransactionTemplate =
        if (readOnly) readOnlyTemplate else readWriteTemplate
}
