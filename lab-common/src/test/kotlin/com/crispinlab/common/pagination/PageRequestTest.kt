package com.crispinlab.common.pagination

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PageRequestTest :
    FunSpec({
        test("offset is page * size") {
            PageRequest(page = 0, size = 20).offset shouldBe 0
            PageRequest(page = 3, size = 20).offset shouldBe 60
        }

        test("page must be non-negative") {
            shouldThrow<IllegalArgumentException> {
                PageRequest(page = -1, size = 20)
            }
        }

        test("size must be within 1..MAX_SIZE") {
            shouldThrow<IllegalArgumentException> { PageRequest(page = 0, size = 0) }
            shouldThrow<IllegalArgumentException> {
                PageRequest(page = 0, size = PageRequest.MAX_SIZE + 1)
            }
        }

        test("firstPage uses default size") {
            val request: PageRequest = PageRequest.firstPage()
            request.page shouldBe 0
            request.size shouldBe PageRequest.DEFAULT_SIZE
        }
    })
