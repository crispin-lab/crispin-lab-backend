package com.crispinlab.space.domain.page

import com.crispinlab.space.testsupport.Fixtures.basicPageLink
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class PageLinkTest :
    DescribeSpec({
        describe("target") {
            it("target 으로 받은 PageId 를 그대로 노출한다") {
                basicPageLink(target = PageId(7L)).target shouldBe PageId(7L)
            }
        }
    })
