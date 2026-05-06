package com.crispinlab.common.exception

class ConflictException(
    message: String,
    cause: Throwable? = null
) : DomainException(message, cause)
