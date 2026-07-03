package com.crispinlab.common.transaction

class RecordingTransactionProvider : TransactionProvider {
    private var depth = 0

    val readOnlyInvocations: MutableList<Boolean> = mutableListOf()

    val inTransaction: Boolean
        get() = depth > 0

    override fun <T> transactional(
        readOnly: Boolean,
        block: () -> T
    ): T {
        readOnlyInvocations += readOnly
        depth++
        return try {
            block()
        } finally {
            depth--
        }
    }

    override fun afterRollback(block: () -> Unit) = Unit
}
