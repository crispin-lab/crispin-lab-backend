package com.crispinlab.common.logging

object LogContext {
    object Mdc {
        const val TRACE_ID: String = "traceId"
        const val METHOD: String = "method"
        const val PATH: String = "path"
    }

    object Field {
        const val STATUS: String = "status"
        const val DURATION_MS: String = "durationMs"
        const val CODE: String = "code"
        const val MESSAGE: String = "message"
        const val CAUSE: String = "cause"
        const val REASON: String = "reason"
    }
}
