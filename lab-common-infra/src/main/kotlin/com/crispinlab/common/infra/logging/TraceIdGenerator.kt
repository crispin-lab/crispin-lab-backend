package com.crispinlab.common.infra.logging

import java.security.SecureRandom
import java.util.HexFormat

class TraceIdGenerator {
    private val random = SecureRandom()

    fun next(): String {
        val bytes = ByteArray(SELF_ISSUED_BYTES)
        random.nextBytes(bytes)
        return HexFormat.of().formatHex(bytes)
    }

    fun traceIdFrom(header: String?): String? {
        if (header.isNullOrBlank()) return null
        val match = TRACEPARENT_PATTERN.matchEntire(header) ?: return null
        return match.groupValues[1].takeIf { it != ALL_ZERO_TRACE_ID }
    }

    companion object {
        private const val SELF_ISSUED_BYTES: Int = 8
        private val TRACEPARENT_PATTERN: Regex =
            Regex("^[0-9a-f]{2}-([0-9a-f]{32})-[0-9a-f]{16}-[0-9a-f]{2}$")
        private const val ALL_ZERO_TRACE_ID: String =
            "00000000000000000000000000000000"
    }
}
