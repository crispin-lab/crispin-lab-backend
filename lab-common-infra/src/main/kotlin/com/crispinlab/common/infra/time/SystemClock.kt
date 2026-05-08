package com.crispinlab.common.infra.time

import com.crispinlab.common.time.Clock
import java.time.Instant
import java.time.Clock as JavaClock

class SystemClock(
    private val delegate: JavaClock = JavaClock.systemUTC()
) : Clock {
    override fun now(): Instant = delegate.instant()
}
