package com.crispinlab.space.application.usecase.access

import com.crispinlab.common.exception.ForbiddenException
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.spacemember.SpaceMember
import com.crispinlab.space.domain.spacemember.SpaceMemberErrorCode
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.space.testsupport.Fixtures.basicSpaceMember
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class SpaceEditPermissionTest :
    DescribeSpec({
        val member = Viewer.Member(userId = UserId(100L), isAdmin = false)
        val admin = Viewer.Member(userId = UserId(100L), isAdmin = true)

        describe("Viewer.canEditSpace") {
            data class Case(
                val label: String,
                val viewer: Viewer,
                val membership: SpaceMember?,
                val expected: Boolean
            )

            listOf(
                Case(
                    label = "Anonymous → false",
                    viewer = Viewer.Anonymous,
                    membership = null,
                    expected = false
                ),
                Case(
                    label = "비멤버 authenticated → false",
                    viewer = member,
                    membership = null,
                    expected = false
                ),
                Case(
                    label = "VIEWER role → false",
                    viewer = member,
                    membership = basicSpaceMember(role = SpaceMemberRole.VIEWER),
                    expected = false
                ),
                Case(
                    label = "MEMBER role → false (canWrite 는 true 지만 canEdit 은 false)",
                    viewer = member,
                    membership = basicSpaceMember(role = SpaceMemberRole.MEMBER),
                    expected = false
                ),
                Case(
                    label = "OWNER role → true",
                    viewer = member,
                    membership = basicSpaceMember(role = SpaceMemberRole.OWNER),
                    expected = true
                ),
                Case(
                    label = "ADMIN 글로벌 → true (membership 무관)",
                    viewer = admin,
                    membership = null,
                    expected = true
                )
            ).forEach { case ->
                it(case.label) {
                    case.viewer.canEditSpace(case.membership) shouldBe case.expected
                }
            }
        }

        describe("SpaceMemberRepository.canEditSpace") {
            val repository = mockk<SpaceMemberRepository>()
            val spaceId = SpaceId(1L)

            beforeEach { clearMocks(repository) }

            it("ADMIN 은 lookup 을 부르지 않고 true") {
                repository.canEditSpace(admin, spaceId) shouldBe true

                verify(exactly = 0) {
                    repository.findBySpaceIdAndUserId(any(), any())
                }
            }

            it("Anonymous 는 lookup 을 부르지 않고 false") {
                repository.canEditSpace(Viewer.Anonymous, spaceId) shouldBe false

                verify(exactly = 0) {
                    repository.findBySpaceIdAndUserId(any(), any())
                }
            }

            it("Member 는 membership 을 조회해 role 로 판정한다 — OWNER 이면 true") {
                every {
                    repository.findBySpaceIdAndUserId(spaceId, member.userId)
                } returns basicSpaceMember(role = SpaceMemberRole.OWNER)

                repository.canEditSpace(member, spaceId) shouldBe true
            }

            it("Member 는 membership 을 조회해 role 로 판정한다 — MEMBER 이면 false") {
                every {
                    repository.findBySpaceIdAndUserId(spaceId, member.userId)
                } returns basicSpaceMember(role = SpaceMemberRole.MEMBER)

                repository.canEditSpace(member, spaceId) shouldBe false
            }

            it("Member 이지만 스페이스 멤버가 아니면 false") {
                every {
                    repository.findBySpaceIdAndUserId(spaceId, member.userId)
                } returns null

                repository.canEditSpace(member, spaceId) shouldBe false
            }
        }

        describe("SpaceMemberRepository.requireSpaceEditPermission") {
            val repository = mockk<SpaceMemberRepository>()
            val spaceId = SpaceId(1L)

            beforeEach { clearMocks(repository) }

            it("허용 케이스 (OWNER) 는 예외 없이 반환한다") {
                every {
                    repository.findBySpaceIdAndUserId(spaceId, member.userId)
                } returns basicSpaceMember(role = SpaceMemberRole.OWNER)

                shouldNotThrowAny {
                    repository.requireSpaceEditPermission(member, spaceId)
                }
            }

            it("거부 케이스 (MEMBER) 는 SPACE_MEMBER_OWNER_ONLY 로 ForbiddenException") {
                every {
                    repository.findBySpaceIdAndUserId(spaceId, member.userId)
                } returns basicSpaceMember(role = SpaceMemberRole.MEMBER)

                val exception =
                    shouldThrow<ForbiddenException> {
                        repository.requireSpaceEditPermission(member, spaceId)
                    }
                exception.errorCode shouldBe SpaceMemberErrorCode.SPACE_MEMBER_OWNER_ONLY
            }

            it("ADMIN 은 lookup 없이 통과한다") {
                shouldNotThrowAny {
                    repository.requireSpaceEditPermission(admin, spaceId)
                }

                verify(exactly = 0) {
                    repository.findBySpaceIdAndUserId(any(), any())
                }
            }
        }
    })
