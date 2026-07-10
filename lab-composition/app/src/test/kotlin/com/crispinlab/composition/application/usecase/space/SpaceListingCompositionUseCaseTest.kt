package com.crispinlab.composition.application.usecase.space

import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.RecordingTransactionProvider
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.composition.application.port.incoming.space.SpaceListingComposition.Request
import com.crispinlab.composition.application.port.outgoing.space.PageStatLookup
import com.crispinlab.composition.application.port.outgoing.space.SpaceMembershipLookup
import com.crispinlab.space.application.port.incoming.space.SpaceListing
import com.crispinlab.space.application.port.incoming.space.SpaceListing.Summary
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository.SortDirection
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository.SortOption
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant

class SpaceListingCompositionUseCaseTest :
    DescribeSpec({
        val spaceListing = mockk<SpaceListing>()
        val spaceMembershipLookup = mockk<SpaceMembershipLookup>()
        val pageStatLookup = mockk<PageStatLookup>()

        fun useCaseWith(transactionProvider: TransactionProvider = RecordingTransactionProvider()) =
            SpaceListingCompositionUseCase(
                spaceListing = spaceListing,
                spaceMembershipLookup = spaceMembershipLookup,
                pageStatLookup = pageStatLookup,
                transactionProvider = transactionProvider
            )

        beforeEach {
            clearMocks(spaceListing, spaceMembershipLookup, pageStatLookup)
            every { spaceMembershipLookup.memberSpaceIdsOf(any()) } returns emptySet()
        }

        describe("스페이스 목록 조립") {
            it("도메인 결과에 myRole·memberCount·pageCount·latestPage 를 붙여 Result 로 반환한다") {
                val member = memberViewer(userId = 100L)
                every { spaceListing.perform(any()) } returns
                    domainListing(
                        summaryOf(
                            spaceId = 10L,
                            name = "팀 위키",
                            updatedAt = SPACE_UPDATED,
                            lastActivityAt = PAGE_UPDATED
                        ),
                        summaryOf(spaceId = 20L, name = "공지", updatedAt = SPACE_UPDATED)
                    )
                every { spaceMembershipLookup.rolesOf(any(), any()) } returns
                    mapOf(SpaceId(10L) to SpaceMemberRole.OWNER)
                every { spaceMembershipLookup.memberCountsOf(any()) } returns
                    mapOf(SpaceId(10L) to 5L, SpaceId(20L) to 3L)
                every { pageStatLookup.countsAndLatestOf(any(), any(), any()) } returns
                    mapOf(
                        SpaceId(10L) to
                            PageStatLookup.PageStat(
                                count = 4L,
                                latest =
                                    PageStatLookup.LatestPage(
                                        pageId = PageId(555L),
                                        title = "회고",
                                        updatedAt = PAGE_UPDATED
                                    )
                            )
                    )

                val result = useCaseWith().perform(basicRequestFor(member))

                val first = result.items[0]
                first.spaceId shouldBe SpaceId(10L)
                first.myRole shouldBe SpaceMemberRole.OWNER
                first.memberCount shouldBe 5L
                first.pageCount shouldBe 4L
                first.latestPage?.pageId shouldBe PageId(555L)
                first.lastActivityAt shouldBe PAGE_UPDATED

                val second = result.items[1]
                second.myRole shouldBe null
                second.memberCount shouldBe 3L
                second.pageCount shouldBe 0L
                second.latestPage shouldBe null
                second.lastActivityAt shouldBe SPACE_UPDATED
            }

            it("distinct spaceIds set 으로 batch lookup 3건을 각각 1회 호출한다 (N+1 방지)") {
                val member = memberViewer(userId = 100L)
                every { spaceListing.perform(any()) } returns
                    domainListing(
                        summaryOf(spaceId = 10L),
                        summaryOf(spaceId = 20L),
                        summaryOf(spaceId = 30L)
                    )
                every { spaceMembershipLookup.rolesOf(any(), any()) } returns emptyMap()
                every { spaceMembershipLookup.memberCountsOf(any()) } returns emptyMap()
                every { pageStatLookup.countsAndLatestOf(any(), any(), any()) } returns emptyMap()

                useCaseWith().perform(basicRequestFor(member))

                val expected = setOf(SpaceId(10L), SpaceId(20L), SpaceId(30L))
                verify(exactly = 1) { spaceMembershipLookup.rolesOf(UserId(100L), expected) }
                verify(exactly = 1) { spaceMembershipLookup.memberCountsOf(expected) }
                verify(exactly = 1) { pageStatLookup.countsAndLatestOf(expected, member, any()) }
                verify(exactly = 1) { spaceMembershipLookup.memberSpaceIdsOf(member) }
            }

            it("memberSpaceIdsOf 실패는 격리하지 않고 그대로 전파한다 (pageStat scope precondition)") {
                val member = memberViewer(userId = 100L)
                every { spaceMembershipLookup.memberSpaceIdsOf(any()) } throws
                    IllegalStateException("멤버십 조회 실패")

                shouldThrow<IllegalStateException> {
                    useCaseWith().perform(basicRequestFor(member))
                }
                verify(exactly = 0) { spaceListing.perform(any()) }
            }

            it("memberSpaceIdsOf 로 lookup 을 한 번만 부르고 그 값을 pageStatLookup 에 전달한다") {
                val member = memberViewer(userId = 100L)
                every { spaceListing.perform(any()) } returns
                    domainListing(summaryOf(spaceId = 10L))
                every { spaceMembershipLookup.memberSpaceIdsOf(any()) } returns
                    setOf(SpaceId(10L), SpaceId(20L))
                every { spaceMembershipLookup.rolesOf(any(), any()) } returns emptyMap()
                every { spaceMembershipLookup.memberCountsOf(any()) } returns emptyMap()
                every { pageStatLookup.countsAndLatestOf(any(), any(), any()) } returns emptyMap()

                useCaseWith().perform(basicRequestFor(member))

                verify(exactly = 1) { spaceMembershipLookup.memberSpaceIdsOf(member) }
                verify(exactly = 1) {
                    pageStatLookup.countsAndLatestOf(
                        setOf(SpaceId(10L)),
                        member,
                        setOf(SpaceId(10L), SpaceId(20L))
                    )
                }
            }

            it("Anonymous viewer 는 myRole 이 항상 null 이고 rolesOf 는 호출되지 않는다") {
                every { spaceListing.perform(any()) } returns
                    domainListing(summaryOf(spaceId = 10L))
                every { spaceMembershipLookup.memberCountsOf(any()) } returns
                    mapOf(SpaceId(10L) to 2L)
                every { pageStatLookup.countsAndLatestOf(any(), any(), any()) } returns emptyMap()

                val result = useCaseWith().perform(basicRequestFor(Viewer.Anonymous))

                result.items.single().myRole shouldBe null
                verify(exactly = 0) { spaceMembershipLookup.rolesOf(any(), any()) }
            }

            it("비-멤버 인증 viewer 는 해당 space 의 myRole 이 null 이다") {
                val member = memberViewer(userId = 100L)
                every { spaceListing.perform(any()) } returns
                    domainListing(summaryOf(spaceId = 10L), summaryOf(spaceId = 20L))
                every { spaceMembershipLookup.rolesOf(any(), any()) } returns
                    mapOf(SpaceId(10L) to SpaceMemberRole.MEMBER)
                every { spaceMembershipLookup.memberCountsOf(any()) } returns emptyMap()
                every { pageStatLookup.countsAndLatestOf(any(), any(), any()) } returns emptyMap()

                val result = useCaseWith().perform(basicRequestFor(member))

                result.items.first { it.spaceId == SpaceId(10L) }.myRole shouldBe
                    SpaceMemberRole.MEMBER
                result.items.first { it.spaceId == SpaceId(20L) }.myRole shouldBe null
            }

            it("memberCountsOf lookup 실패는 memberCount=0 으로 degrade, 나머지 필드는 유지") {
                val member = memberViewer(userId = 100L)
                every { spaceListing.perform(any()) } returns
                    domainListing(summaryOf(spaceId = 10L))
                every { spaceMembershipLookup.rolesOf(any(), any()) } returns
                    mapOf(SpaceId(10L) to SpaceMemberRole.OWNER)
                every { spaceMembershipLookup.memberCountsOf(any()) } throws
                    IllegalStateException("멤버 조회 실패")
                every { pageStatLookup.countsAndLatestOf(any(), any(), any()) } returns
                    mapOf(SpaceId(10L) to PageStatLookup.PageStat(count = 7L, latest = null))

                val result = useCaseWith().perform(basicRequestFor(member))

                val first = result.items.single()
                first.memberCount shouldBe 0L
                first.myRole shouldBe SpaceMemberRole.OWNER
                first.pageCount shouldBe 7L
            }

            it("pageStatLookup 실패는 pageCount=0 + latestPage=null 로 degrade") {
                val member = memberViewer(userId = 100L)
                every { spaceListing.perform(any()) } returns
                    domainListing(summaryOf(spaceId = 10L, updatedAt = SPACE_UPDATED))
                every { spaceMembershipLookup.rolesOf(any(), any()) } returns emptyMap()
                every { spaceMembershipLookup.memberCountsOf(any()) } returns
                    mapOf(SpaceId(10L) to 2L)
                every { pageStatLookup.countsAndLatestOf(any(), any(), any()) } throws
                    RuntimeException("page stat 실패")

                val result = useCaseWith().perform(basicRequestFor(member))

                val first = result.items.single()
                first.pageCount shouldBe 0L
                first.latestPage shouldBe null
                first.lastActivityAt shouldBe SPACE_UPDATED
                first.memberCount shouldBe 2L
            }

            it("rolesOf 실패는 myRole=null 로 degrade") {
                val member = memberViewer(userId = 100L)
                every { spaceListing.perform(any()) } returns
                    domainListing(summaryOf(spaceId = 10L))
                every { spaceMembershipLookup.rolesOf(any(), any()) } throws
                    IllegalStateException("role 조회 실패")
                every { spaceMembershipLookup.memberCountsOf(any()) } returns emptyMap()
                every { pageStatLookup.countsAndLatestOf(any(), any(), any()) } returns emptyMap()

                val result = useCaseWith().perform(basicRequestFor(member))

                result.items.single().myRole shouldBe null
            }

            it("keyword / sort / direction 을 도메인 UseCase 로 그대로 forward 한다") {
                val member = memberViewer(userId = 100L)
                every { spaceListing.perform(any()) } returns domainListing()
                every { spaceMembershipLookup.rolesOf(any(), any()) } returns emptyMap()
                every { spaceMembershipLookup.memberCountsOf(any()) } returns emptyMap()
                every { pageStatLookup.countsAndLatestOf(any(), any(), any()) } returns emptyMap()

                useCaseWith().perform(
                    basicRequestFor(
                        viewer = member,
                        keyword = "위키",
                        sort = "NAME",
                        direction = "ASC"
                    )
                )

                verify {
                    spaceListing.perform(
                        withArg {
                            it.keyword shouldBe "위키"
                            it.sort shouldBe SortOption.NAME
                            it.direction shouldBe SortDirection.ASC
                        }
                    )
                }
            }

            it("perform 진입에서 readOnly 트랜잭션으로 감싸고 모든 lookup 이 tx 블록 안에서 실행된다") {
                val transactionProvider = RecordingTransactionProvider()
                every { spaceMembershipLookup.memberSpaceIdsOf(any()) } answers {
                    transactionProvider.inTransaction shouldBe true
                    emptySet()
                }
                every { spaceListing.perform(any()) } answers {
                    transactionProvider.inTransaction shouldBe true
                    domainListing(summaryOf(spaceId = 10L))
                }
                every { spaceMembershipLookup.rolesOf(any(), any()) } answers {
                    transactionProvider.inTransaction shouldBe true
                    emptyMap()
                }
                every { spaceMembershipLookup.memberCountsOf(any()) } answers {
                    transactionProvider.inTransaction shouldBe true
                    emptyMap()
                }
                every { pageStatLookup.countsAndLatestOf(any(), any(), any()) } answers {
                    transactionProvider.inTransaction shouldBe true
                    emptyMap()
                }

                useCaseWith(transactionProvider).perform(basicRequestFor(memberViewer(100L)))

                transactionProvider.readOnlyInvocations shouldBe listOf(true)
            }
        }
    }) {
    companion object {
        private val SPACE_UPDATED: Instant = DUMMY_INSTANT
        private val PAGE_UPDATED: Instant = DUMMY_INSTANT.plusSeconds(3600)

        fun memberViewer(
            userId: Long,
            isAdmin: Boolean = false
        ): Viewer.Member = Viewer.Member(userId = UserId(userId), isAdmin = isAdmin)

        fun basicRequestFor(
            viewer: Viewer,
            keyword: String? = null,
            sort: String? = null,
            direction: String? = null
        ): Request =
            Request(
                keyword = keyword,
                sort = sort,
                direction = direction,
                page = 0,
                size = 20,
                viewer = viewer
            )

        fun summaryOf(
            spaceId: Long,
            name: String = "스페이스",
            description: String = "설명",
            visibility: SpaceVisibility = SpaceVisibility.PUBLIC,
            updatedAt: Instant = DUMMY_INSTANT,
            lastActivityAt: Instant = updatedAt
        ): Summary =
            Summary(
                spaceId = SpaceId(spaceId),
                name = name,
                description = description,
                visibility = visibility,
                lastActivityAt = lastActivityAt,
                createdAt = DUMMY_INSTANT,
                updatedAt = updatedAt
            )

        fun domainListing(vararg summaries: Summary): PageResult<Summary> =
            PageResult(
                items = summaries.toList(),
                page = 0,
                size = 20,
                totalElements = summaries.size.toLong()
            )
    }
}
