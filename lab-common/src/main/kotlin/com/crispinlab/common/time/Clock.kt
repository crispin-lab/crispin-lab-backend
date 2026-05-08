package com.crispinlab.common.time

import java.time.Instant

interface Clock {
    fun now(): Instant
}
