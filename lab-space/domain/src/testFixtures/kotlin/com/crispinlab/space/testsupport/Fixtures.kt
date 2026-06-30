package com.crispinlab.space.testsupport

import com.crispinlab.space.domain.comment.Comment
import com.crispinlab.space.domain.comment.CommentContent
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.domain.mention.Mention
import com.crispinlab.space.domain.mention.MentionId
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageContent
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageLink
import com.crispinlab.space.domain.page.PageLinkId
import com.crispinlab.space.domain.page.PageRevision
import com.crispinlab.space.domain.page.PageRevisionId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.domain.spacemember.SpaceMember
import com.crispinlab.space.domain.spacemember.SpaceMemberId
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.space.domain.tag.PageTag
import com.crispinlab.space.domain.tag.Tag
import com.crispinlab.space.domain.tag.TagId
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.user.domain.user.UserId
import java.time.Instant

object Fixtures {
    fun basicSpace(
        id: SpaceId = SpaceId(1L),
        name: String = "자유게시판",
        description: String = "기본 설명",
        visibility: SpaceVisibility = SpaceVisibility.INTERNAL,
        createdAt: Instant = DUMMY_INSTANT,
        deletedAt: Instant? = null
    ): Space =
        Space(
            id = id,
            name = name,
            description = description,
            visibility = visibility,
            createdAt = createdAt,
            deletedAt = deletedAt
        )

    fun basicPage(
        id: PageId = PageId(1L),
        spaceId: SpaceId = SpaceId(10L),
        parentPageId: PageId? = null,
        authorId: UserId = UserId(100L),
        title: String = "초안",
        content: PageContent = PageContent("본문"),
        visibility: Visibility = Visibility.DRAFT,
        currentVersion: Int = 1,
        displayOrder: Int = 0,
        createdAt: Instant = DUMMY_INSTANT,
        updatedAt: Instant = createdAt,
        deletedAt: Instant? = null
    ): Page =
        Page(
            id = id,
            spaceId = spaceId,
            parentPageId = parentPageId,
            authorId = authorId,
            title = title,
            content = content,
            visibility = visibility,
            currentVersion = currentVersion,
            displayOrder = displayOrder,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt
        )

    fun basicPageRevision(
        id: PageRevisionId = PageRevisionId(1L),
        pageId: PageId = PageId(10L),
        version: Int = 1,
        title: String = "초안",
        content: PageContent = PageContent("본문"),
        authorId: UserId = UserId(100L),
        createdAt: Instant = DUMMY_INSTANT
    ): PageRevision =
        PageRevision(
            id = id,
            pageId = pageId,
            version = version,
            title = title,
            content = content,
            authorId = authorId,
            createdAt = createdAt
        )

    fun basicPageLink(
        id: PageLinkId = PageLinkId(1L),
        pageId: PageId = PageId(10L),
        revisionId: PageRevisionId = PageRevisionId(100L),
        target: PageId = PageId(20L),
        createdAt: Instant = DUMMY_INSTANT
    ): PageLink =
        PageLink(
            id = id,
            pageId = pageId,
            revisionId = revisionId,
            target = target,
            createdAt = createdAt
        )

    fun basicComment(
        id: CommentId = CommentId(1L),
        pageId: PageId = PageId(10L),
        authorId: UserId = UserId(100L),
        content: CommentContent = CommentContent("댓글"),
        createdAt: Instant = DUMMY_INSTANT,
        deletedAt: Instant? = null
    ): Comment =
        Comment(
            id = id,
            pageId = pageId,
            authorId = authorId,
            content = content,
            createdAt = createdAt,
            deletedAt = deletedAt
        )

    fun basicMention(
        id: MentionId = MentionId(1L),
        sourceType: Mention.SourceType = Mention.SourceType.PAGE,
        sourceId: Long = 10L,
        mentionedUserId: UserId = UserId(200L),
        mentionedByUserId: UserId = UserId(100L),
        createdAt: Instant = DUMMY_INSTANT
    ): Mention =
        Mention(
            id = id,
            sourceType = sourceType,
            sourceId = sourceId,
            mentionedUserId = mentionedUserId,
            mentionedByUserId = mentionedByUserId,
            createdAt = createdAt
        )

    fun basicTag(
        id: TagId = TagId(1L),
        spaceId: SpaceId = SpaceId(10L),
        name: String = "kotlin",
        createdAt: Instant = DUMMY_INSTANT
    ): Tag =
        Tag(
            id = id,
            spaceId = spaceId,
            name = name,
            createdAt = createdAt
        )

    fun basicPageTag(
        pageId: PageId = PageId(10L),
        tagId: TagId = TagId(100L),
        createdAt: Instant = DUMMY_INSTANT
    ): PageTag =
        PageTag(
            pageId = pageId,
            tagId = tagId,
            createdAt = createdAt
        )

    fun basicSpaceMember(
        id: SpaceMemberId = SpaceMemberId(1L),
        spaceId: SpaceId = SpaceId(10L),
        userId: UserId = UserId(100L),
        role: SpaceMemberRole = SpaceMemberRole.MEMBER,
        joinedAt: Instant = DUMMY_INSTANT
    ): SpaceMember =
        SpaceMember(
            id = id,
            spaceId = spaceId,
            userId = userId,
            role = role,
            joinedAt = joinedAt
        )
}
