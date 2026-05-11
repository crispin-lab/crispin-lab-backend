package com.crispinlab.apisupport.testsupport

import com.crispinlab.common.application.UseCase
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document
import com.epages.restdocs.apispec.ResourceSnippetParametersBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import io.kotest.common.KotestInternal
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import kotlin.text.Charsets.UTF_8
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.restdocs.ManualRestDocumentation
import org.springframework.restdocs.cookies.CookieDescriptor
import org.springframework.restdocs.cookies.CookieDocumentation.cookieWithName
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.JsonFieldType.ARRAY
import org.springframework.restdocs.payload.JsonFieldType.BOOLEAN
import org.springframework.restdocs.payload.JsonFieldType.NUMBER
import org.springframework.restdocs.payload.JsonFieldType.OBJECT
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.payload.RequestFieldsSnippet
import org.springframework.restdocs.payload.RequestPartFieldsSnippet
import org.springframework.restdocs.payload.ResponseFieldsSnippet
import org.springframework.restdocs.request.ParameterDescriptor
import org.springframework.restdocs.request.QueryParametersSnippet
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.restdocs.snippet.Snippet
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import org.springframework.web.method.support.HandlerMethodArgumentResolver

/**
 * 컨트롤러 테스트 베이스. Standalone MockMvc + ManualRestDocumentation 으로 묶여 있어
 * `@SpringBootApplication` 자동 검색·`@WebMvcTest` 슬라이스 없이 동작한다.
 *
 * 영역별로 `BaseControllerDescribeSpec` 을 두고 `argumentResolvers`·`controllerAdvices` 를 wiring 한 뒤,
 * 각 컨트롤러 테스트는 그 베이스를 상속해 `responseFields {}` / `requestFields {}` 같은
 * `FieldBuilder` DSL 로 OpenAPI 산출을 만든다.
 */
abstract class ControllerDescribeSpec(
    private val service: String? = null,
    private val tag: String,
    private val argumentResolvers: List<HandlerMethodArgumentResolver> = emptyList(),
    private val controllerAdvices: List<Any> = emptyList(),
    body: ControllerDescribeSpec.() -> Unit = {}
) : DescribeSpec() {
    private val documentation = ManualRestDocumentation()
    private lateinit var identifier: String
    private lateinit var description: String

    init {
        @OptIn(KotestInternal::class)
        beforeEach { testCase ->
            identifier =
                listOfNotNull(
                    tag,
                    testCase.descriptor.testParts().joinToString(" ")
                ).joinToString(" ")
            description =
                testCase.descriptor
                    .testParts()
                    .let { parts ->
                        parts.take(parts.size - 1).joinToString(" ")
                    }

            documentation.beforeTest(
                testCase.javaClass,
                testCase.name.name
            )
        }

        afterEach {
            documentation.afterTest()
        }

        @Suppress("UNUSED_EXPRESSION")
        body()
    }

    private fun <T> T.mockMvc(): MockMvc {
        val objectMapper: ObjectMapper =
            Jackson2ObjectMapperBuilder
                .json()
                .featuresToDisable(WRITE_DATES_AS_TIMESTAMPS)
                .build()

        return MockMvcBuilders
            .standaloneSetup(this)
            .setCustomArgumentResolvers(*argumentResolvers.toTypedArray())
            .setControllerAdvice(*controllerAdvices.toTypedArray())
            .setMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
            .apply<StandaloneMockMvcBuilder>(documentationConfiguration(documentation))
            .build()
    }

    fun <T> T.`when`(builder: RequestBuilder): ResultActions =
        mockMvc()
            .perform(builder)
            .andDo(MockMvcResultHandlers.print())

    fun ResultActions.then(vararg resultMatchers: ResultMatcher): ResultActions =
        resultMatchers.fold(this) { resultActions, resultMatcher ->
            resultActions.andExpect(resultMatcher)
        }

    fun MockHttpServletRequestBuilder.withUserHeader(
        userId: String = "100"
    ): MockHttpServletRequestBuilder = header(USER_ID_HEADER, userId)

    inline fun <reified T> MockHttpServletRequestBuilder.body(
        body: T
    ): MockHttpServletRequestBuilder {
        val mapper: ObjectMapper =
            Jackson2ObjectMapperBuilder
                .json()
                .featuresToDisable(WRITE_DATES_AS_TIMESTAMPS)
                .build()
        return content(mapper.writeValueAsString(body))
            .contentType(APPLICATION_JSON)
            .characterEncoding(UTF_8)
    }

    infix fun String.isParameterFor(description: String): ParameterDescriptor =
        parameterWithName(this).description(description)

    infix fun ParameterDescriptor.isOptional(optional: Boolean): ParameterDescriptor =
        if (optional) this.optional() else this

    infix fun String.isCookieFor(description: String): CookieDescriptor =
        cookieWithName(this).description(description)

    fun pagingParameters(
        pageOptional: Boolean = true,
        sizeOptional: Boolean = true
    ): QueryParametersSnippet =
        queryParameters(
            "page" isParameterFor "페이지" isOptional pageOptional,
            "size" isParameterFor "페이지당 항목 수" isOptional sizeOptional
        )

    fun QueryParametersSnippet.withPaging(
        pageOptional: Boolean = true,
        sizeOptional: Boolean = true
    ): QueryParametersSnippet =
        and(
            "page" isParameterFor "페이지" isOptional pageOptional,
            "size" isParameterFor "페이지당 항목 수" isOptional sizeOptional
        )

    private fun basicDocument(vararg snippets: Snippet): RestDocumentationResultHandler =
        document(
            identifier = identifier,
            resourceDetails =
                ResourceSnippetParametersBuilder()
                    .tag(
                        listOfNotNull(
                            service?.let { "[$it]" },
                            tag
                        ).joinToString(" ")
                    ).description(description),
            snippets = snippets
        )

    fun ResultActions.document(vararg snippets: Snippet): ResultActions =
        andDo(basicDocument(snippets = snippets))

    fun userHeaderRequired(optional: Boolean = false): Snippet =
        requestHeaders(
            headerWithName(USER_ID_HEADER)
                .description("사용자 인증 헤더 (현재 임시)")
                .let { if (optional) it.optional() else it }
        )

    class FieldBuilder(
        private val pathPrefix: String = "",
        private val fields: MutableList<Field> = mutableListOf(),
        private val indent: Int = 0
    ) {
        private val fieldDescriptors: List<FieldDescriptor>
            get() = fields.flatMap { it.fieldDescriptors() }

        private fun toResponseFieldsSnippet(): ResponseFieldsSnippet =
            PayloadDocumentation.responseFields(fieldDescriptors)

        private fun toRequestFieldsSnippet(): RequestFieldsSnippet =
            PayloadDocumentation.requestFields(fieldDescriptors)

        private fun toRequestPartFieldsSnippet(partName: String): RequestPartFieldsSnippet =
            PayloadDocumentation.requestPartFields(partName, fieldDescriptors)

        private fun add(field: Field) {
            fields += field
        }

        fun array(description: String) {
            BasicField(
                path = "$pathPrefix[]",
                type = ARRAY,
                description = description,
                indent = indent
            ).let { add(it) }
        }

        fun array(build: FieldBuilder.() -> Unit = {}) {
            FieldBuilder("$pathPrefix[].")
                .apply(build)
                .fields
                .forEach { add(it) }
        }

        fun String.array(
            description: String,
            optional: Boolean = false,
            ignoreBody: Boolean = false,
            build: FieldBuilder.() -> Unit = {}
        ) {
            "$pathPrefix$this".let { path ->
                when (ignoreBody) {
                    false -> {
                        BasicField(
                            path = path,
                            type = ARRAY,
                            description = description,
                            optional = optional,
                            indent = indent,
                            children =
                                FieldBuilder("$path[].", indent = indent + 1)
                                    .apply(build)
                                    .fields
                        ).let { add(it) }
                    }

                    true -> {
                        SubsectionField(
                            path = "$pathPrefix$this",
                            type = ARRAY,
                            description = description,
                            optional = optional,
                            indent = indent + 1
                        ).let { add(it) }
                    }
                }
            }
        }

        fun String.`object`(
            description: String,
            optional: Boolean = false,
            ignoreBody: Boolean = false,
            build: FieldBuilder.() -> Unit = {}
        ) {
            "$pathPrefix$this".let { path ->
                when (ignoreBody) {
                    false -> {
                        BasicField(
                            path = path,
                            type = OBJECT,
                            description = description,
                            optional = optional,
                            indent = indent,
                            children =
                                FieldBuilder("$path.", indent = indent + 1)
                                    .apply(build)
                                    .fields
                        ).let { add(it) }
                    }

                    true -> {
                        SubsectionField(
                            path = "$pathPrefix$this",
                            type = OBJECT,
                            description = description,
                            optional = optional,
                            indent = indent + 1
                        ).let { add(it) }
                    }
                }
            }
        }

        fun String.string(
            description: String,
            optional: Boolean = false
        ) {
            BasicField(
                path = "$pathPrefix$this",
                type = STRING,
                optional = optional,
                description = description,
                indent = indent
            ).let { add(it) }
        }

        fun String.datetime(
            description: String,
            optional: Boolean = false
        ) {
            string(description = "$description (ISO)", optional = optional)
        }

        fun String.number(
            description: String,
            optional: Boolean = false
        ) {
            BasicField(
                path = "$pathPrefix$this",
                type = NUMBER,
                optional = optional,
                description = description,
                indent = indent
            ).let { add(it) }
        }

        fun String.boolean(
            description: String,
            optional: Boolean = false
        ) {
            BasicField(
                path = "$pathPrefix$this",
                type = BOOLEAN,
                optional = optional,
                description = description,
                indent = indent
            ).let { add(it) }
        }

        fun String.period(
            name: String = "기간",
            optional: Boolean = false
        ) {
            `object`(name, optional = optional) {
                "from".string("시작일")
                "to".string("종료일")
            }
        }

        interface Field {
            val path: String
            val type: JsonFieldType
            val description: String
            val optional: Boolean
            val indent: Int
            val decoratedDescription: String
                get() {
                    val builder = StringBuilder()
                    repeat(indent) { builder.append("⎯ ") }
                    builder.append(
                        when (optional) {
                            false -> "$description＊"
                            true -> description
                        }
                    )
                    return builder.toString()
                }

            fun fieldDescriptors(): List<FieldDescriptor>
        }

        private data class BasicField(
            override val path: String,
            override val type: JsonFieldType,
            override val description: String,
            override val optional: Boolean = false,
            override val indent: Int = 0,
            val children: List<Field> = emptyList()
        ) : Field {
            override fun fieldDescriptors(): List<FieldDescriptor> =
                listOf(
                    fieldWithPath(path)
                        .type(type)
                        .description(decoratedDescription)
                        .also {
                            if (optional) {
                                it.optional()
                            }
                        }
                ) +
                    children.flatMap { it.fieldDescriptors() }
        }

        private data class SubsectionField(
            override val path: String,
            override val type: JsonFieldType,
            override val description: String,
            override val optional: Boolean = false,
            override val indent: Int = 0
        ) : Field {
            override fun fieldDescriptors(): List<FieldDescriptor> =
                listOf(
                    subsectionWithPath(path)
                        .type(type)
                        .description(decoratedDescription)
                        .let { if (optional) it.optional() else it }
                )
        }

        companion object {
            private fun fields(build: FieldBuilder.() -> Unit): FieldBuilder =
                FieldBuilder().apply { build() }

            fun responseFields(build: FieldBuilder.() -> Unit): ResponseFieldsSnippet =
                fields(build).toResponseFieldsSnippet()

            fun requestFields(build: FieldBuilder.() -> Unit): RequestFieldsSnippet =
                fields(build).toRequestFieldsSnippet()

            fun requestPartFields(
                partName: String,
                build: FieldBuilder.() -> Unit
            ): RequestPartFieldsSnippet =
                fields(build).toRequestPartFieldsSnippet(partName = partName)
        }
    }

    companion object {
        const val USER_ID_HEADER: String = "X-User-Id"

        @JvmStatic
        protected fun FieldBuilder.importPaging(
            pageOptional: Boolean = false,
            sizeOptional: Boolean = false
        ) {
            "totalItems".number("총 항목 수")
            "page".number("현재 페이지", optional = pageOptional)
            "size".number("페이지당 항목 수", optional = sizeOptional)
        }

        inline fun <reified T : UseCase<R, Unit>, reified R : Any> successfulUseCase(): T {
            val mock: T = mockk()
            every { mock.perform(any<R>()) } returns Unit
            return mock
        }

        inline fun <reified T : UseCase<R, S>, reified R : Any, S> successfulUseCase(
            block: () -> S
        ): T {
            val mock: T = mockk()
            every { mock.perform(any<R>()) } returns block()
            return mock
        }
    }
}
