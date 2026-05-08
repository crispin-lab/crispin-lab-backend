package com.crispinlab.common.infra.transaction

import com.crispinlab.common.transaction.TransactionProvider
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

class DefaultTransactionProvider(
    transactionManager: PlatformTransactionManager
) : TransactionProvider {
    private val readWriteTemplate: TransactionTemplate = TransactionTemplate(transactionManager)
    private val readOnlyTemplate: TransactionTemplate =
        TransactionTemplate(transactionManager).apply {
            isReadOnly = true
        }

    override fun <T> transactional(
        readOnly: Boolean,
        block: () -> T
    ): T =
        templateFor(readOnly).execute {
            block()
        }

    private fun templateFor(readOnly: Boolean): TransactionTemplate =
        if (readOnly) readOnlyTemplate else readWriteTemplate
}
