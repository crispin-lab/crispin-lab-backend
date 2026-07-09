package com.crispinlab.space.application.usecase.audit

import com.crispinlab.common.exception.ForbiddenException
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.space.application.port.incoming.audit.SpaceAuditEntryListing.Request
import com.crispinlab.space.application.port.outgoing.audit.SpaceAuditRepository
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.audit.SpaceAuditAction
import com.crispinlab.space.domain.audit.SpaceAuditEntryId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.space.testsupport.Fixtures.basicSpace
import com.crispinlab.space.testsupport.Fixtures.basicSpaceAuditEntry
import com.crispinlab.space.testsupport.Fixtures.basicSpaceMember
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class SpaceAuditEntryListingUseCaseTest :
    DescribeSpec({
        val spaceRepository = mockk<SpaceRepository>()
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val spaceAuditRepository = mockk<SpaceAuditRepository>()
        val useCase =
            SpaceAuditEntryListingUseCase(
                spaceRepository = spaceRepository,
                spaceMemberRepository = spaceMemberRepository,
                spaceAuditRepository = spaceAuditRepository,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(spaceRepository, spaceMemberRepository, spaceAuditRepository)
        }

        describe("스페이스 감사 이력 조회") {
            it("OWNER 는 최신순 감사 이력을 페이지로 받는다") {
                every { spaceRepository.findBy(SpaceId(1L)) } returns basicSpace(id = SpaceId(1L))
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(any(), any())
                } returns basicSpaceMember(role = SpaceMemberRole.OWNER)
                every {
                    spaceAuditRepository.findBySpaceId(SpaceId(1L), any())
                } returns
                    PageResult(
                        items =
                            listOf(
                                basicSpaceAuditEntry(
                                    id = SpaceAuditEntryId(2L),
                                    action = SpaceAuditAction.EDITED
                                ),
                                basicSpaceAuditEntry(
                                    id = SpaceAuditEntryId(1L),
                                    action = SpaceAuditAction.REGISTERED
                                )
                            ),
                        page = 0,
                        size = 20,
                        totalElements = 2L
                    )

                val result = useCase.perform(basicRequest())

                result.items shouldHaveSize 2
                result.items[0].id shouldBe SpaceAuditEntryId(2L)
                result.items[0].action shouldBe SpaceAuditAction.EDITED
                result.items[1].action shouldBe SpaceAuditAction.REGISTERED
            }

            it("ADMIN 은 space 존재 검증 없이 감사 이력을 조회할 수 있다") {
                every {
                    spaceAuditRepository.findBySpaceId(any(), any())
                } returns PageResult(items = emptyList(), page = 0, size = 20, totalElements = 0L)

                useCase.perform(basicRequest(isAdmin = true))

                verify(exactly = 0) { spaceRepository.findBy(any()) }
                verify(exactly = 0) {
                    spaceMemberRepository.findBySpaceIdAndUserId(any(), any())
                }
                verify(exactly = 1) { spaceAuditRepository.findBySpaceId(any(), any()) }
            }

            it("편집 권한자가 삭제된 스페이스를 조회하면 NotFoundException") {
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(any(), any())
                } returns basicSpaceMember(role = SpaceMemberRole.OWNER)
                every { spaceRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { spaceAuditRepository.findBySpaceId(any(), any()) }
            }

            it("MEMBER 는 ForbiddenException") {
                every { spaceRepository.findBy(any()) } returns basicSpace()
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(any(), any())
                } returns basicSpaceMember(role = SpaceMemberRole.MEMBER)

                shouldThrow<ForbiddenException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { spaceAuditRepository.findBySpaceId(any(), any()) }
            }

            it("멤버가 아니면 ForbiddenException") {
                every { spaceRepository.findBy(any()) } returns basicSpace()
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(any(), any())
                } returns null

                shouldThrow<ForbiddenException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { spaceAuditRepository.findBySpaceId(any(), any()) }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            spaceId: String = "1",
            page: Int = 0,
            size: Int = PageRequest.DEFAULT_SIZE,
            userId: UserId = UserId(100L),
            isAdmin: Boolean = false
        ): Request =
            Request(
                spaceId = spaceId,
                page = page,
                size = size,
                viewer = Viewer.Member(userId = userId, isAdmin = isAdmin)
            )
    }
}
