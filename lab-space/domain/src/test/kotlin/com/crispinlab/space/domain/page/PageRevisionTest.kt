package com.crispinlab.space.domain.page

import com.crispinlab.space.testsupport.Fixtures.basicPageRevision
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec

class PageRevisionTest :
    DescribeSpec({
        describe("init") {
            it("정상 생성") {
                basicPageRevision()
            }

            it("version 이 1 미만이면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicPageRevision(version = 0)
                }
            }

            it("title 이 비어 있으면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicPageRevision(title = "")
                }
            }

            it("title 이 ${Page.MAX_TITLE_LENGTH}자를 넘으면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicPageRevision(title = "a".repeat(Page.MAX_TITLE_LENGTH + 1))
                }
            }
        }
    })
