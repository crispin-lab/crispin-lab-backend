package com.crispinlab.space.adapter.persistence.mention

import com.crispinlab.common.persistence.PostgresTestContext
import com.crispinlab.space.domain.mention.Mention
import com.crispinlab.space.domain.mention.MentionId
import com.crispinlab.space.testsupport.Fixtures.basicMention
import com.crispinlab.user.domain.user.UserId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExposedMentionRepositoryTest :
    DescribeSpec({
        val database = PostgresTestContext.database
        val repository = ExposedMentionRepository()

        afterEach {
            PostgresTestContext.truncateAll()
        }

        describe("ExposedMentionRepository") {
            it("replaceMentionsFor(emptyList) 은 아무 것도 저장하지 않는다") {
                transaction(database) {
                    val newlyAdded =
                        repository.replaceMentionsFor(
                            sourceType = Mention.SourceType.PAGE,
                            sourceId = 10L,
                            mentions = emptyList()
                        )

                    newlyAdded.shouldBeEmpty()
                }

                transaction(database) {
                    repository.findBy(Mention.SourceType.PAGE, 10L).shouldBeEmpty()
                }
            }

            it("최초 replace 는 모든 mention 을 신규로 반환한다") {
                val mentions =
                    listOf(
                        basicMention(
                            id = MentionId(1L),
                            sourceType = Mention.SourceType.PAGE,
                            sourceId = 10L,
                            mentionedUserId = UserId(200L)
                        ),
                        basicMention(
                            id = MentionId(2L),
                            sourceType = Mention.SourceType.PAGE,
                            sourceId = 10L,
                            mentionedUserId = UserId(201L)
                        )
                    )

                val newlyAdded =
                    transaction(database) {
                        repository.replaceMentionsFor(
                            sourceType = Mention.SourceType.PAGE,
                            sourceId = 10L,
                            mentions = mentions
                        )
                    }

                newlyAdded.map { it.mentionedUserId } shouldContainExactlyInAnyOrder
                    listOf(UserId(200L), UserId(201L))
                transaction(database) {
                    repository.findBy(Mention.SourceType.PAGE, 10L) shouldHaveSize 2
                }
            }

            it("재 replace 시 기존 row 는 삭제되고 신규 user 만 반환된다") {
                transaction(database) {
                    repository.replaceMentionsFor(
                        sourceType = Mention.SourceType.PAGE,
                        sourceId = 10L,
                        mentions =
                            listOf(
                                basicMention(
                                    id = MentionId(1L),
                                    mentionedUserId = UserId(200L)
                                ),
                                basicMention(
                                    id = MentionId(2L),
                                    mentionedUserId = UserId(201L)
                                )
                            )
                    )
                }

                val newlyAdded =
                    transaction(database) {
                        repository.replaceMentionsFor(
                            sourceType = Mention.SourceType.PAGE,
                            sourceId = 10L,
                            mentions =
                                listOf(
                                    basicMention(
                                        id = MentionId(3L),
                                        mentionedUserId = UserId(201L)
                                    ),
                                    basicMention(
                                        id = MentionId(4L),
                                        mentionedUserId = UserId(202L)
                                    )
                                )
                        )
                    }

                newlyAdded.map { it.mentionedUserId } shouldBe listOf(UserId(202L))
                transaction(database) {
                    repository.findBy(Mention.SourceType.PAGE, 10L).map {
                        it.mentionedUserId
                    } shouldContainExactlyInAnyOrder listOf(UserId(201L), UserId(202L))
                }
            }

            it("같은 sourceId 라도 다른 sourceType 은 격리된다") {
                transaction(database) {
                    repository.replaceMentionsFor(
                        sourceType = Mention.SourceType.PAGE,
                        sourceId = 10L,
                        mentions =
                            listOf(
                                basicMention(
                                    id = MentionId(1L),
                                    sourceType = Mention.SourceType.PAGE,
                                    sourceId = 10L,
                                    mentionedUserId = UserId(200L)
                                )
                            )
                    )
                    repository.replaceMentionsFor(
                        sourceType = Mention.SourceType.COMMENT,
                        sourceId = 10L,
                        mentions =
                            listOf(
                                basicMention(
                                    id = MentionId(2L),
                                    sourceType = Mention.SourceType.COMMENT,
                                    sourceId = 10L,
                                    mentionedUserId = UserId(300L)
                                )
                            )
                    )
                }

                transaction(database) {
                    repository.findBy(Mention.SourceType.PAGE, 10L).map {
                        it.mentionedUserId
                    } shouldBe listOf(UserId(200L))
                    repository.findBy(Mention.SourceType.COMMENT, 10L).map {
                        it.mentionedUserId
                    } shouldBe listOf(UserId(300L))
                }
            }
        }
    })
