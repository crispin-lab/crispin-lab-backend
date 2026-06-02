package com.crispinlab.common.transaction

class DummyTransactionProvider : TransactionProvider {
    override fun <T> transactional(
        readOnly: Boolean,
        block: () -> T
    ): T = block()

    override fun afterRollback(block: () -> Unit) = Unit
}
