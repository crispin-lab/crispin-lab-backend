package com.crispinlab.space.adapter.web

import com.crispinlab.common.exception.ConflictException
import com.crispinlab.common.exception.DomainException
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.logging.LogContext.Field
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(exception: NotFoundException): ResponseEntity<ErrorPayload> =
        respondClientError(
            status = HttpStatus.NOT_FOUND,
            code = exception.errorCode.code,
            message = exception.message
        )

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(exception: ConflictException): ResponseEntity<ErrorPayload> =
        respondClientError(
            status = HttpStatus.CONFLICT,
            code = exception.errorCode.code,
            message = exception.message
        )

    @ExceptionHandler(DomainException::class)
    fun handleDomain(exception: DomainException): ResponseEntity<ErrorPayload> =
        respondClientError(
            status = HttpStatus.UNPROCESSABLE_ENTITY,
            code = exception.errorCode.code,
            message = exception.message
        )

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(exception: IllegalArgumentException): ResponseEntity<ErrorPayload> =
        respondClientError(
            status = HttpStatus.BAD_REQUEST,
            code = INVALID_REQUEST,
            message = exception.message
        )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(exception: MethodArgumentNotValidException): ResponseEntity<ErrorPayload> =
        respondClientError(
            status = HttpStatus.BAD_REQUEST,
            code = INVALID_REQUEST,
            message = exception.bindingResult.fieldError?.defaultMessage ?: "요청 값이 올바르지 않습니다."
        )

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleNotReadable(
        exception: HttpMessageNotReadableException
    ): ResponseEntity<ErrorPayload> =
        respondClientError(
            status = HttpStatus.BAD_REQUEST,
            code = INVALID_REQUEST,
            message = "요청 본문을 읽을 수 없습니다.",
            cause = exception
        )

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParameter(
        exception: MissingServletRequestParameterException
    ): ResponseEntity<ErrorPayload> =
        respondClientError(
            status = HttpStatus.BAD_REQUEST,
            code = INVALID_REQUEST,
            message = "필수 파라미터가 누락되었습니다.",
            cause = exception
        )

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        exception: MethodArgumentTypeMismatchException
    ): ResponseEntity<ErrorPayload> =
        respondClientError(
            status = HttpStatus.BAD_REQUEST,
            code = INVALID_REQUEST,
            message = "요청 파라미터 형식이 올바르지 않습니다.",
            cause = exception
        )

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(exception: Exception): ResponseEntity<ErrorPayload> {
        log.error("처리되지 않은 예외", exception)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorPayload(code = INTERNAL_ERROR, message = INTERNAL_ERROR_MESSAGE))
    }

    private fun respondClientError(
        status: HttpStatus,
        code: String,
        message: String?,
        cause: Throwable? = null
    ): ResponseEntity<ErrorPayload> {
        val responseMessage: String = message ?: DEFAULT_MESSAGE
        log.warn(
            "클라이언트 오류 {}={} {}={} {}={} {}={}",
            Field.STATUS,
            status.value(),
            Field.CODE,
            code,
            Field.MESSAGE,
            responseMessage,
            Field.CAUSE,
            cause?.message ?: "-"
        )
        return ResponseEntity
            .status(status)
            .body(ErrorPayload(code = code, message = responseMessage))
    }

    data class ErrorPayload(
        val code: String,
        val message: String
    )

    companion object {
        private const val INVALID_REQUEST: String = "INVALID_REQUEST"
        private const val INTERNAL_ERROR: String = "INTERNAL_ERROR"
        private const val INTERNAL_ERROR_MESSAGE: String = "서버 오류가 발생했습니다."
        private const val DEFAULT_MESSAGE: String = "요청을 처리할 수 없습니다."
    }
}
