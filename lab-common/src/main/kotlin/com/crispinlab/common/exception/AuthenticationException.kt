package com.crispinlab.common.exception

class AuthenticationException(
    errorCode: ErrorCode,
    message: String = errorCode.defaultMessage,
    cause: Throwable? = null
) : DomainException(errorCode, message, cause)
