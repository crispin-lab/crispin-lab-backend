package com.crispinlab.common.exception

abstract class DomainException(
    val errorCode: ErrorCode,
    message: String = errorCode.defaultMessage,
    cause: Throwable? = null
) : RuntimeException(message, cause)
