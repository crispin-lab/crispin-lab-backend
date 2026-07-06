package com.crispinlab.space.adapter.persistence.visibility

import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.user.domain.user.UserId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class VisibilityScopePredicateTest :
    DescribeSpec({
        describe("VisibilityScope.toClauses()") {
            it("Anonymous 는 PUBLIC page × PUBLIC space 한 clause 만 산출한다") {
                val clauses = VisibilityScope.Anonymous.toClauses()

                clauses shouldBe
                    listOf(
                        VisibilityClause(
                            atoms =
                                listOf(
                                    VisibilityAtom.Eq(
                                        column = VisibilityColumn.PageVisibility,
                                        value = Visibility.PUBLIC.name
                                    ),
                                    VisibilityAtom.Eq(
                                        column = VisibilityColumn.SpaceVisibility,
                                        value = SpaceVisibility.PUBLIC.name
                                    )
                                )
                        )
                    )
            }

            it(
                "Authenticated 비멤버는 PUBLIC + INTERNAL 자기 + PUBLIC/MEMBER 자기 INTERNAL space + DRAFT 자기 네 clause"
            ) {
                val viewerId = UserId(100L)

                val clauses =
                    VisibilityScope
                        .Authenticated(
                            viewerId = viewerId,
                            memberOfSpaceIds = emptySet()
                        ).toClauses()

                clauses.size shouldBe 4
                clauses[0].atoms shouldBe
                    listOf(
                        VisibilityAtom.Eq(
                            column = VisibilityColumn.PageVisibility,
                            value = Visibility.PUBLIC.name
                        ),
                        VisibilityAtom.Eq(
                            column = VisibilityColumn.SpaceVisibility,
                            value = SpaceVisibility.PUBLIC.name
                        )
                    )
                clauses[1].atoms shouldBe
                    listOf(
                        VisibilityAtom.Eq(
                            column = VisibilityColumn.PageVisibility,
                            value = Visibility.INTERNAL.name
                        ),
                        VisibilityAtom.Eq(
                            column = VisibilityColumn.PageAuthorId,
                            value = viewerId.value
                        )
                    )
                clauses[2].atoms shouldBe
                    listOf(
                        VisibilityAtom.In(
                            column = VisibilityColumn.PageVisibility,
                            values = listOf(Visibility.PUBLIC.name, Visibility.MEMBER.name)
                        ),
                        VisibilityAtom.Eq(
                            column = VisibilityColumn.SpaceVisibility,
                            value = SpaceVisibility.INTERNAL.name
                        ),
                        VisibilityAtom.Eq(
                            column = VisibilityColumn.PageAuthorId,
                            value = viewerId.value
                        )
                    )
                clauses[3].atoms shouldBe
                    listOf(
                        VisibilityAtom.Eq(
                            column = VisibilityColumn.PageVisibility,
                            value = Visibility.DRAFT.name
                        ),
                        VisibilityAtom.Eq(
                            column = VisibilityColumn.PageAuthorId,
                            value = viewerId.value
                        )
                    )
            }

            it("Authenticated 멤버는 MEMBER page × PUBLIC space clause 가 추가된다") {
                val viewerId = UserId(100L)
                val memberSpaceIds = setOf(SpaceId(10L), SpaceId(20L))

                val clauses =
                    VisibilityScope
                        .Authenticated(
                            viewerId = viewerId,
                            memberOfSpaceIds = memberSpaceIds
                        ).toClauses()

                clauses.size shouldBe 5
                clauses[1].atoms shouldBe
                    listOf(
                        VisibilityAtom.Eq(
                            column = VisibilityColumn.PageVisibility,
                            value = Visibility.MEMBER.name
                        ),
                        VisibilityAtom.Eq(
                            column = VisibilityColumn.SpaceVisibility,
                            value = SpaceVisibility.PUBLIC.name
                        ),
                        VisibilityAtom.In(
                            column = VisibilityColumn.PageSpaceId,
                            values = memberSpaceIds.map { it.value }
                        )
                    )
            }

            it("Privileged 는 항상 참인 EMPTY clause 를 하나 산출한다") {
                val clauses = VisibilityScope.Privileged.toClauses()

                clauses shouldBe listOf(VisibilityClause.ALWAYS)
                clauses.single().atoms shouldBe emptyList()
            }
        }
    })
