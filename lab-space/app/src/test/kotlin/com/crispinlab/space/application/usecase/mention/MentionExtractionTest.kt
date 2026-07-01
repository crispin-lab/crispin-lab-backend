package com.crispinlab.space.application.usecase.mention

import com.crispinlab.space.domain.comment.CommentContent
import com.crispinlab.space.domain.page.PageContent
import com.crispinlab.space.testsupport.TipTapJsonFixtures.bulletList
import com.crispinlab.space.testsupport.TipTapJsonFixtures.doc
import com.crispinlab.space.testsupport.TipTapJsonFixtures.listItem
import com.crispinlab.space.testsupport.TipTapJsonFixtures.mention
import com.crispinlab.space.testsupport.TipTapJsonFixtures.mentionWithRawAttrs
import com.crispinlab.space.testsupport.TipTapJsonFixtures.paragraph
import com.crispinlab.space.testsupport.TipTapJsonFixtures.text
import com.crispinlab.user.domain.user.UserId
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class MentionExtractionTest :
    DescribeSpec({
        val mapper = ObjectMapper()

        describe("PageContent.extractMentions") {
            it("mention 노드가 없는 빈 doc 는 빈 리스트") {
                val content = PageContent("""{"type":"doc","content":[]}""")

                content.extractMentions(mapper).shouldBeEmpty()
            }

            it("paragraph 안의 단일 mention 노드를 추출한다") {
                val content =
                    PageContent(
                        doc(
                            paragraph(
                                text("안녕 "),
                                mention(userId = 42L),
                                text(", 확인 부탁드려요")
                            )
                        )
                    )

                content.extractMentions(mapper) shouldContainExactly listOf(UserId(42L))
            }

            it("여러 paragraph 의 다중 mention 을 모두 추출한다") {
                val content =
                    PageContent(
                        doc(
                            paragraph(mention(userId = 1L)),
                            paragraph(
                                text("중간 "),
                                mention(userId = 2L)
                            )
                        )
                    )

                content.extractMentions(mapper) shouldBe listOf(UserId(1L), UserId(2L))
            }

            it("동일 userId 가 여러 번 등장해도 한 번만 추출한다") {
                val content =
                    PageContent(
                        doc(
                            paragraph(mention(userId = 7L)),
                            paragraph(mention(userId = 7L)),
                            paragraph(mention(userId = 8L))
                        )
                    )

                content.extractMentions(mapper) shouldBe listOf(UserId(7L), UserId(8L))
            }

            it("userId 형식이 잘못된 mention 은 자연 drop 된다") {
                val content =
                    PageContent(
                        doc(
                            paragraph(
                                mentionWithRawAttrs(""""userId":"not-a-number""""),
                                mention(userId = 99L)
                            )
                        )
                    )

                content.extractMentions(mapper) shouldBe listOf(UserId(99L))
            }

            it("nested bulletList/listItem 안의 mention 도 깊이에 관계없이 추출한다") {
                val content =
                    PageContent(
                        doc(
                            bulletList(
                                listItem(paragraph(mention(userId = 11L))),
                                listItem(paragraph(mention(userId = 12L)))
                            )
                        )
                    )

                content.extractMentions(mapper) shouldBe listOf(UserId(11L), UserId(12L))
            }

            it("malformed JSON 본문은 빈 리스트로 fallback") {
                val content = PageContent("not-json-at-all")

                content.extractMentions(mapper).shouldBeEmpty()
            }
        }

        describe("CommentContent.extractMentions") {
            it("paragraph 안의 mention 을 추출한다") {
                val content =
                    CommentContent(
                        doc(
                            paragraph(
                                text("답변 "),
                                mention(userId = 33L)
                            )
                        )
                    )

                content.extractMentions(mapper) shouldContainExactly listOf(UserId(33L))
            }

            it("malformed JSON 은 빈 리스트로 fallback") {
                val content = CommentContent("plain text")

                content.extractMentions(mapper).shouldBeEmpty()
            }
        }
    })
