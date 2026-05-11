package com.crispinlab.space.adapter.web

import com.crispinlab.common.exception.ConflictException
import com.crispinlab.common.exception.NotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(exception: NotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(message = exception.message ?: DEFAULT_MESSAGE))

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(exception: ConflictException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ErrorResponse(message = exception.message ?: DEFAULT_MESSAGE))

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(exception: IllegalArgumentException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = exception.message ?: DEFAULT_MESSAGE))

    data class ErrorResponse(
        val message: String
    )

    companion object {
        private const val DEFAULT_MESSAGE: String = "요청을 처리할 수 없습니다."
    }
}
