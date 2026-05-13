package com.crispinlab.common.infra.logging

import com.crispinlab.common.logging.LogContext.Field
import com.crispinlab.common.logging.LogContext.Mdc
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.web.filter.OncePerRequestFilter

class TraceContextFilter(
    private val generator: TraceIdGenerator
) : OncePerRequestFilter() {
    override fun shouldNotFilterErrorDispatch(): Boolean = true

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        val traceId = generator.traceIdFrom(request.getHeader("traceparent")) ?: generator.next()
        val start = System.nanoTime()
        val previous: Map<String, String>? = MDC.getCopyOfContextMap()

        MDC.put(Mdc.TRACE_ID, traceId)
        MDC.put(Mdc.METHOD, request.method)
        MDC.put(Mdc.PATH, request.requestURI)
        response.setHeader("X-Trace-Id", traceId)

        try {
            chain.doFilter(request, response)
        } finally {
            val durationMs = (System.nanoTime() - start) / NANOS_PER_MILLI
            accessLog.info(
                "{}={} {}={}",
                Field.STATUS,
                response.status,
                Field.DURATION_MS,
                durationMs
            )
            previous?.let { MDC.setContextMap(it) } ?: MDC.clear()
        }
    }

    companion object {
        const val ACCESS_LOGGER_NAME: String = "http.access"
        private const val NANOS_PER_MILLI: Long = 1_000_000L
        private val accessLog = LoggerFactory.getLogger(ACCESS_LOGGER_NAME)
    }
}
