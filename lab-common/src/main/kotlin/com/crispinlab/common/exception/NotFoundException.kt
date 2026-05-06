package com.crispinlab.common.exception

class NotFoundException(
    message: String,
    cause: Throwable? = null
) : DomainException(message, cause) {
    companion object {
        fun of(
            entity: String,
            identifier: Any
        ): NotFoundException = NotFoundException("$entity not found: $identifier")
    }
}
