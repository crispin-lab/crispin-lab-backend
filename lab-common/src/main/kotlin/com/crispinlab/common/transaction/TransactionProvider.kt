package com.crispinlab.common.transaction

interface TransactionProvider {
    fun <T> transactional(
        readOnly: Boolean = false,
        block: () -> T
    ): T

    fun afterRollback(block: () -> Unit)
}
