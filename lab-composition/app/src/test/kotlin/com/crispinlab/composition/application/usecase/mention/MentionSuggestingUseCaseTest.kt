package com.crispinlab.composition.application.usecase.mention

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.RecordingTransactionProvider
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.composition.application.port.incoming.mention.MentionSuggesting.Request
import com.crispinlab.composition.application.port.outgoing.space.SpaceMembershipLookup
import com.crispinlab.composition.application.port.outgoing.user.UserAdminLookup
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.application.port.incoming.user.UserSearching
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class MentionSuggestingUseCaseTest :
    DescribeSpec({
        val userSearching = mockk<UserSearching>()
        val spaceMembershipLookup = mockk<SpaceMembershipLookup>()
        val userAdminLookup = mockk<UserAdminLookup>()

        fun useCaseWith(transactionProvider: TransactionProvider = RecordingTransactionProvider()) =
            MentionSuggestingUseCase(
                userSearching = userSearching,
                spaceMembershipLookup = spaceMembershipLookup,
                userAdminLookup = userAdminLookup,
                transactionProvider = transactionProvider
            )

        beforeEach {
            clearMocks(userSearching, spaceMembershipLookup, userAdminLookup)
            every { spaceMembershipLookup.memberSpaceIdsOf(any()) } returns
                setOf(SpaceId(TARGET_SPACE_ID))
        }

        describe("mention 후보 조회") {
            describe("접근 권한 검증") {
                it("검색자가 대상 스페이스 비멤버면 NotFoundException 을 던진다 (IDOR 보호)") {
                    every { spaceMembershipLookup.memberSpaceIdsOf(MEMBER_VIEWER) } returns
                        emptySet()

                    shouldThrow<NotFoundException> {
                        useCaseWith().perform(basicRequest())
                    }
                    verify(exactly = 0) { userSearching.perform(any()) }
                    verify(exactly = 0) { userAdminLookup.adminsAmong(any()) }
                    verify(exactly = 0) { spaceMembershipLookup.membershipsOf(any(), any()) }
                }

                it("ADMIN 검색자는 대상 스페이스 비멤버여도 통과한다 (memberSpaceIdsOf 조회 skip)") {
                    every { userSearching.perform(any()) } returns
                        UserSearching.Result(items = emptyList())

                    useCaseWith().perform(
                        basicRequest(viewer = ADMIN_VIEWER)
                    )

                    verify(exactly = 0) { spaceMembershipLookup.memberSpaceIdsOf(any()) }
                }
            }

            describe("visibility 필터") {
                it("PUBLIC 페이지 × PUBLIC 스페이스 는 비멤버 후보도 통과한다") {
                    every { userSearching.perform(any()) } returns
                        UserSearching.Result(
                            items =
                                listOf(
                                    domainItem(userId = 1L, handle = "alice"),
                                    domainItem(userId = 2L, handle = "bob")
                                )
                        )
                    every { userAdminLookup.adminsAmong(any()) } returns emptySet()
                    every { spaceMembershipLookup.membershipsOf(any(), any()) } returns emptyMap()

                    val result =
                        useCaseWith().perform(
                            basicRequest(
                                pageVisibility = "PUBLIC",
                                spaceVisibility = "PUBLIC"
                            )
                        )

                    result.items.map { it.userId } shouldBe listOf(UserId(1L), UserId(2L))
                }

                it("MEMBER 페이지 는 대상 스페이스 소속 후보만 통과한다") {
                    every { userSearching.perform(any()) } returns
                        UserSearching.Result(
                            items =
                                listOf(
                                    domainItem(userId = 1L, handle = "alice"),
                                    domainItem(userId = 2L, handle = "bob")
                                )
                        )
                    every { userAdminLookup.adminsAmong(any()) } returns emptySet()
                    every { spaceMembershipLookup.membershipsOf(any(), any()) } returns
                        mapOf(UserId(1L) to setOf(SpaceId(TARGET_SPACE_ID)))

                    val result =
                        useCaseWith().perform(
                            basicRequest(
                                pageVisibility = "MEMBER",
                                spaceVisibility = "PUBLIC"
                            )
                        )

                    result.items.map { it.userId } shouldBe listOf(UserId(1L))
                }

                it("INTERNAL 페이지 는 pageAuthorId 후보만 통과한다") {
                    every { userSearching.perform(any()) } returns
                        UserSearching.Result(
                            items =
                                listOf(
                                    domainItem(userId = PAGE_AUTHOR_ID, handle = "author"),
                                    domainItem(userId = 2L, handle = "bob")
                                )
                        )
                    every { userAdminLookup.adminsAmong(any()) } returns emptySet()
                    every { spaceMembershipLookup.membershipsOf(any(), any()) } returns emptyMap()

                    val result =
                        useCaseWith().perform(basicRequest(pageVisibility = "INTERNAL"))

                    result.items.map { it.userId } shouldBe listOf(UserId(PAGE_AUTHOR_ID))
                }

                it("DRAFT 페이지 는 pageAuthorId 후보만 통과한다") {
                    every { userSearching.perform(any()) } returns
                        UserSearching.Result(
                            items =
                                listOf(
                                    domainItem(userId = PAGE_AUTHOR_ID, handle = "author"),
                                    domainItem(userId = 2L, handle = "bob")
                                )
                        )
                    every { userAdminLookup.adminsAmong(any()) } returns emptySet()
                    every { spaceMembershipLookup.membershipsOf(any(), any()) } returns emptyMap()

                    val result =
                        useCaseWith().perform(basicRequest(pageVisibility = "DRAFT"))

                    result.items.map { it.userId } shouldBe listOf(UserId(PAGE_AUTHOR_ID))
                }

                it("INTERNAL 스페이스 × PUBLIC 페이지 는 스페이스 멤버만 통과한다 (SpaceVisibility.ceiling)") {
                    every { userSearching.perform(any()) } returns
                        UserSearching.Result(
                            items =
                                listOf(
                                    domainItem(userId = 1L, handle = "alice"),
                                    domainItem(userId = 2L, handle = "bob")
                                )
                        )
                    every { userAdminLookup.adminsAmong(any()) } returns emptySet()
                    every { spaceMembershipLookup.membershipsOf(any(), any()) } returns
                        mapOf(UserId(2L) to setOf(SpaceId(TARGET_SPACE_ID)))

                    val result =
                        useCaseWith().perform(
                            basicRequest(
                                pageVisibility = "PUBLIC",
                                spaceVisibility = "INTERNAL"
                            )
                        )

                    result.items.map { it.userId } shouldBe listOf(UserId(2L))
                }

                it("ADMIN 후보는 visibility 무관 항상 통과한다 (Privileged)") {
                    every { userSearching.perform(any()) } returns
                        UserSearching.Result(
                            items =
                                listOf(
                                    domainItem(userId = 1L, handle = "alice"),
                                    domainItem(userId = 2L, handle = "bob_admin")
                                )
                        )
                    every { userAdminLookup.adminsAmong(any()) } returns setOf(UserId(2L))
                    every { spaceMembershipLookup.membershipsOf(any(), any()) } returns emptyMap()

                    val result =
                        useCaseWith().perform(basicRequest(pageVisibility = "DRAFT"))

                    result.items.map { it.userId } shouldBe listOf(UserId(2L))
                }
            }

            describe("조회·조립") {
                it("userSearching 은 size × CANDIDATE_MULTIPLIER 로 호출하되 MAX_SIZE=20 로 clamp 한다") {
                    val requestSlot = slot<UserSearching.Request>()
                    every { userSearching.perform(capture(requestSlot)) } returns
                        UserSearching.Result(items = emptyList())
                    every { userAdminLookup.adminsAmong(any()) } returns emptySet()
                    every { spaceMembershipLookup.membershipsOf(any(), any()) } returns emptyMap()

                    useCaseWith().perform(basicRequest(size = 5))

                    requestSlot.captured.size shouldBe 15
                }

                it("size × 3 이 20 을 넘으면 MAX_SIZE=20 으로 clamp 된다") {
                    val requestSlot = slot<UserSearching.Request>()
                    every { userSearching.perform(capture(requestSlot)) } returns
                        UserSearching.Result(items = emptyList())
                    every { userAdminLookup.adminsAmong(any()) } returns emptySet()
                    every { spaceMembershipLookup.membershipsOf(any(), any()) } returns emptyMap()

                    useCaseWith().perform(basicRequest(size = 10))

                    requestSlot.captured.size shouldBe UserSearching.Request.MAX_SIZE
                }

                it("필터 후 개수가 size 를 넘으면 상한으로 truncate 한다") {
                    every { userSearching.perform(any()) } returns
                        UserSearching.Result(
                            items =
                                (1L..8L).map {
                                    domainItem(userId = it, handle = "user_$it")
                                }
                        )
                    every { userAdminLookup.adminsAmong(any()) } returns emptySet()
                    every { spaceMembershipLookup.membershipsOf(any(), any()) } returns emptyMap()

                    val result =
                        useCaseWith().perform(
                            basicRequest(
                                size = 3,
                                pageVisibility = "PUBLIC",
                                spaceVisibility = "PUBLIC"
                            )
                        )

                    result.items.size shouldBe 3
                }

                it("distinct userIds 로 adminsAmong / membershipsOf 를 각각 batch 1회 호출한다") {
                    every { userSearching.perform(any()) } returns
                        UserSearching.Result(
                            items =
                                listOf(
                                    domainItem(userId = 1L, handle = "alice"),
                                    domainItem(userId = 1L, handle = "alice"),
                                    domainItem(userId = 2L, handle = "bob")
                                )
                        )
                    every { userAdminLookup.adminsAmong(any()) } returns emptySet()
                    every { spaceMembershipLookup.membershipsOf(any(), any()) } returns emptyMap()

                    useCaseWith().perform(basicRequest())

                    verify(exactly = 1) {
                        userAdminLookup.adminsAmong(setOf(UserId(1L), UserId(2L)))
                    }
                    verify(exactly = 1) {
                        spaceMembershipLookup.membershipsOf(
                            userIds = setOf(UserId(1L), UserId(2L)),
                            viewer = MEMBER_VIEWER
                        )
                    }
                }

                it(
                    "perform 진입에서 readOnly 트랜잭션으로 감싸고 도메인 호출·lookup 모두 tx 블록 안에서 실행한다 (LAB-156 회귀 방지)"
                ) {
                    val transactionProvider = RecordingTransactionProvider()
                    every { spaceMembershipLookup.memberSpaceIdsOf(any()) } answers {
                        transactionProvider.inTransaction shouldBe true
                        setOf(SpaceId(TARGET_SPACE_ID))
                    }
                    every { userSearching.perform(any()) } answers {
                        transactionProvider.inTransaction shouldBe true
                        UserSearching.Result(
                            items = listOf(domainItem(userId = 1L, handle = "alice"))
                        )
                    }
                    every { userAdminLookup.adminsAmong(any()) } answers {
                        transactionProvider.inTransaction shouldBe true
                        emptySet()
                    }
                    every { spaceMembershipLookup.membershipsOf(any(), any()) } answers {
                        transactionProvider.inTransaction shouldBe true
                        emptyMap()
                    }

                    useCaseWith(transactionProvider).perform(
                        basicRequest(pageVisibility = "PUBLIC", spaceVisibility = "PUBLIC")
                    )

                    transactionProvider.readOnlyInvocations shouldBe listOf(true)
                }

                it("검색 결과가 비면 lookup 을 호출하지 않고 빈 items 를 반환한다") {
                    every { userSearching.perform(any()) } returns
                        UserSearching.Result(items = emptyList())

                    val result = useCaseWith().perform(basicRequest())

                    result.items shouldBe emptyList()
                    verify(exactly = 0) { userAdminLookup.adminsAmong(any()) }
                    verify(exactly = 0) { spaceMembershipLookup.membershipsOf(any(), any()) }
                }
            }

            describe("Request 형식 검증") {
                it("size 가 상한을 넘으면 IllegalArgumentException 을 던진다") {
                    shouldThrow<IllegalArgumentException> {
                        basicRequest(size = UserSearching.Request.MAX_SIZE + 1)
                    }
                }

                it("size 가 0 이하이면 IllegalArgumentException 을 던진다") {
                    shouldThrow<IllegalArgumentException> {
                        basicRequest(size = 0)
                    }
                }
            }

            describe("실패 전파") {
                it("adminsAmong 이 예외를 던지면 그대로 전파한다 (lookup 격리 미적용)") {
                    every { userSearching.perform(any()) } returns
                        UserSearching.Result(
                            items = listOf(domainItem(userId = 1L, handle = "alice"))
                        )
                    every { userAdminLookup.adminsAmong(any()) } throws
                        RuntimeException("lookup failure")

                    shouldThrow<RuntimeException> {
                        useCaseWith().perform(basicRequest())
                    }
                }

                it("membershipsOf 가 예외를 던지면 그대로 전파한다 (lookup 격리 미적용)") {
                    every { userSearching.perform(any()) } returns
                        UserSearching.Result(
                            items = listOf(domainItem(userId = 1L, handle = "alice"))
                        )
                    every { userAdminLookup.adminsAmong(any()) } returns emptySet()
                    every { spaceMembershipLookup.membershipsOf(any(), any()) } throws
                        RuntimeException("lookup failure")

                    shouldThrow<RuntimeException> {
                        useCaseWith().perform(basicRequest())
                    }
                }
            }
        }
    }) {
    companion object {
        const val TARGET_SPACE_ID: Long = 10L
        const val PAGE_AUTHOR_ID: Long = 100L
        val MEMBER_VIEWER: Viewer.Member =
            Viewer.Member(userId = UserId(500L), isAdmin = false)
        val ADMIN_VIEWER: Viewer.Member =
            Viewer.Member(userId = UserId(999L), isAdmin = true)

        fun basicRequest(
            query: String = "al",
            size: Int = 5,
            spaceId: String = "$TARGET_SPACE_ID",
            spaceVisibility: String = "PUBLIC",
            pageVisibility: String = "PUBLIC",
            pageAuthorId: String = "$PAGE_AUTHOR_ID",
            viewer: Viewer.Member = MEMBER_VIEWER
        ): Request =
            Request(
                query = query,
                size = size,
                spaceId = spaceId,
                spaceVisibility = spaceVisibility,
                pageVisibility = pageVisibility,
                pageAuthorId = pageAuthorId,
                viewer = viewer
            )

        fun domainItem(
            userId: Long,
            handle: String
        ): UserSearching.Result.Item =
            UserSearching.Result.Item(
                userId = UserId(userId),
                handle = Handle(handle)
            )
    }
}
