package com.crispinlab.common.pagination

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PageResultTest :
    FunSpec({
        test("totalPages rounds up the last partial page") {
            PageResult(
                items = listOf(1),
                page = 0,
                size = 10,
                totalElements = 25
            ).totalPages shouldBe
                3
            PageResult(
                items = listOf(1),
                page = 0,
                size = 10,
                totalElements = 30
            ).totalPages shouldBe
                3
            PageResult(
                items = listOf(1),
                page = 0,
                size = 10,
                totalElements = 31
            ).totalPages shouldBe
                4
        }

        test("totalPages is 0 when there are no elements") {
            PageResult<Int>(
                items = emptyList(),
                page = 0,
                size = 10,
                totalElements = 0L
            ).totalPages shouldBe 0
        }

        test("hasNext reflects whether more pages remain") {
            PageResult(items = listOf(1), page = 0, size = 10, totalElements = 25).hasNext shouldBe
                true
            PageResult(items = listOf(1), page = 2, size = 10, totalElements = 25).hasNext shouldBe
                false
        }

        test("map transforms items while preserving pagination metadata") {
            val original: PageResult<Int> =
                PageResult(items = listOf(1, 2, 3), page = 1, size = 3, totalElements = 10)
            val mapped: PageResult<Int> = original.map { it * 10 }

            mapped.items shouldBe listOf(10, 20, 30)
            mapped.page shouldBe 1
            mapped.size shouldBe 3
            mapped.totalElements shouldBe 10L
        }

        test("rejects invalid constructor arguments") {
            shouldThrow<IllegalArgumentException> {
                PageResult<Int>(items = emptyList(), page = -1, size = 10, totalElements = 0L)
            }
            shouldThrow<IllegalArgumentException> {
                PageResult<Int>(items = emptyList(), page = 0, size = 0, totalElements = 0L)
            }
            shouldThrow<IllegalArgumentException> {
                PageResult<Int>(items = emptyList(), page = 0, size = 10, totalElements = -1L)
            }
        }

        test("empty creates a result with zero items but the requested pagination") {
            val result: PageResult<String> = PageResult.empty(PageRequest(page = 1, size = 10))

            result.items shouldBe emptyList()
            result.page shouldBe 1
            result.size shouldBe 10
            result.totalElements shouldBe 0L
            result.totalPages shouldBe 0
            result.hasNext shouldBe false
        }
    })
