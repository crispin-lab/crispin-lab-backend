package com.crispinlab

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ApplicationTest :
    DescribeSpec({
        extensions(SpringExtension())

        describe("Spring 컨텍스트") {
            it("정상적으로 로드된다") {
            }
        }
    })
