package com.crispinlab.user.domain.credential

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CredentialTest :
    DescribeSpec({
        describe("PasswordHash") {
            it("정상 값이면 생성된다") {
                val raw = "\$2a\$12\$" + "a".repeat(53)
                val hash = PasswordHash(raw)

                hash.value shouldBe raw
            }

            it("빈 값이면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    PasswordHash("")
                }
            }

            it("공백만 있어도 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    PasswordHash("   ")
                }
            }
        }

        // Credential.OAuth 의 subjectId 검증 (isNotBlank / MAX_SUBJECT_ID_LENGTH) 회귀는
        // OAuthProvider 의 항목이 추가되는 후속 PR 에서 함께 작성한다.
        // 본 PR 에서는 OAuthProvider 항목이 비어 있어 OAuth 인스턴스를 만들 수 없다.
    })
