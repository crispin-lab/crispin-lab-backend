package com.crispinlab.space.adapter.web

import com.crispinlab.common.exception.DomainException
import com.crispinlab.common.exception.ErrorCode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.http.MockHttpInputMessage
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException

class GlobalExceptionHandlerTest :
    DescribeSpec({
        val handler = GlobalExceptionHandler()

        describe("핸들러 fallback") {
            it("DomainException 은 422 + errorCode.code 로 매핑한다") {
                val response =
                    handler.handleDomain(object : DomainException(DummyErrorCode.DUMMY_FAILURE) {})

                response.statusCode shouldBe HttpStatus.UNPROCESSABLE_ENTITY
                val body = response.body.shouldNotBeNull()
                body.code shouldBe "DUMMY_FAILURE"
                body.message shouldBe "도메인 규칙을 위반했습니다."
            }

            it("IllegalArgumentException 은 400 + INVALID_REQUEST + raw 메시지로 매핑한다") {
                val response =
                    handler.handleIllegalArgument(
                        IllegalArgumentException("페이지 ID 형식이 올바르지 않습니다.")
                    )

                response.statusCode shouldBe HttpStatus.BAD_REQUEST
                val body = response.body.shouldNotBeNull()
                body.code shouldBe "INVALID_REQUEST"
                body.message shouldBe "페이지 ID 형식이 올바르지 않습니다."
            }

            it("MethodArgumentNotValidException 은 400 + 첫 필드 오류 메시지로 매핑한다") {
                val bindingResult = BeanPropertyBindingResult(DummyBody(title = ""), "body")
                bindingResult.rejectValue("title", "NotBlank", "제목을 입력해 주세요.")
                val methodParameter =
                    MethodParameter(String::class.java.getDeclaredMethod("isEmpty"), -1)
                val exception = MethodArgumentNotValidException(methodParameter, bindingResult)

                val response = handler.handleValidation(exception)

                response.statusCode shouldBe HttpStatus.BAD_REQUEST
                val body = response.body.shouldNotBeNull()
                body.code shouldBe "INVALID_REQUEST"
                body.message shouldBe "제목을 입력해 주세요."
            }

            it("HttpMessageNotReadableException 은 400 + 고정 메시지로 매핑한다") {
                val response =
                    handler.handleNotReadable(
                        HttpMessageNotReadableException(
                            "malformed",
                            MockHttpInputMessage(ByteArray(0))
                        )
                    )

                response.statusCode shouldBe HttpStatus.BAD_REQUEST
                val body = response.body.shouldNotBeNull()
                body.code shouldBe "INVALID_REQUEST"
                body.message shouldBe "요청 본문을 읽을 수 없습니다."
            }

            it("MissingServletRequestParameterException 은 400 + 고정 메시지로 매핑한다") {
                val response =
                    handler.handleMissingParameter(
                        MissingServletRequestParameterException("page", "Int")
                    )

                response.statusCode shouldBe HttpStatus.BAD_REQUEST
                val body = response.body.shouldNotBeNull()
                body.code shouldBe "INVALID_REQUEST"
                body.message shouldBe "필수 파라미터가 누락되었습니다."
            }

            it("처리되지 않은 Exception 은 500 + 마스킹 메시지로 매핑한다") {
                val response = handler.handleUnexpected(RuntimeException("내부 스택 노출 위험"))

                response.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
                val body = response.body.shouldNotBeNull()
                body.code shouldBe "INTERNAL_ERROR"
                body.message shouldBe "서버 오류가 발생했습니다."
            }
        }
    }) {
    private enum class DummyErrorCode(
        override val defaultMessage: String
    ) : ErrorCode {
        DUMMY_FAILURE("도메인 규칙을 위반했습니다.")
        ;

        override val code: String get() = name
    }

    private data class DummyBody(
        val title: String
    )
}
