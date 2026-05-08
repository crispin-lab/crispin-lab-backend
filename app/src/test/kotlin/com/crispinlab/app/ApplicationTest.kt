package com.crispinlab.app

import com.crispinlab.common.transaction.TransactionProvider
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("local")
class ApplicationTest : DescribeSpec() {
    @Autowired
    private lateinit var transactionProvider: TransactionProvider

    init {
        extensions(SpringExtension())

        describe("Spring 컨텍스트") {
            it("정상적으로 로드되고 TransactionProvider 빈이 주입된다") {
                ::transactionProvider.isInitialized shouldBe true
            }
        }
    }
}
