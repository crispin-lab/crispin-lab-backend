package com.crispinlab.space.domain.space

import com.crispinlab.space.domain.page.Visibility
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class SpaceVisibilityTest :
    DescribeSpec({
        describe("ceiling — space audience 안에 담을 수 있는 최대 page visibility") {
            it("PUBLIC space 는 PUBLIC page 까지 담는다") {
                SpaceVisibility.PUBLIC.ceiling() shouldBe Visibility.PUBLIC
            }

            it("INTERNAL space 는 MEMBER page 까지 담는다 (audience: 스페이스 멤버 ⊆ 로그인 사용자 전원)") {
                SpaceVisibility.INTERNAL.ceiling() shouldBe Visibility.MEMBER
            }
        }
    })
