package com.crispinlab.user.testsupport

import com.crispinlab.common.transaction.TransactionProvider

class DummyTransactionProvider : TransactionProvider {
    override fun <T> transactional(
        readOnly: Boolean,
        block: () -> T
    ): T = block()

    override fun afterRollback(block: () -> Unit) = Unit
}
