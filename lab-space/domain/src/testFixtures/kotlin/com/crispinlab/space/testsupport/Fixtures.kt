package com.crispinlab.space.testsupport

import com.crispinlab.space.domain.comment.Comment
import com.crispinlab.space.domain.comment.CommentId
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
import com.crispinlab.space.domain.tag.PageTag
import com.crispinlab.space.domain.tag.Tag
import com.crispinlab.space.domain.tag.TagId
import com.crispinlab.space.domain.user.UserId
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import java.time.Instant

object Fixtures {
    fun basicSpace(
        id: SpaceId = SpaceId(1L),
        name: String = "자유게시판",
        description: String = "기본 설명",
        createdAt: Instant = DUMMY_INSTANT,
        deletedAt: Instant? = null
    ): Space =
        Space(
            id = id,
            name = name,
            description = description,
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
        createdAt: Instant = DUMMY_INSTANT,
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
            createdAt = createdAt,
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
        target: String = "다른 페이지",
        type: PageLink.Type = PageLink.Type.INTERNAL,
        createdAt: Instant = DUMMY_INSTANT
    ): PageLink =
        PageLink(
            id = id,
            pageId = pageId,
            revisionId = revisionId,
            target = target,
            type = type,
            createdAt = createdAt
        )

    fun basicComment(
        id: CommentId = CommentId(1L),
        pageId: PageId = PageId(10L),
        authorId: UserId = UserId(100L),
        body: String = "댓글",
        createdAt: Instant = DUMMY_INSTANT,
        deletedAt: Instant? = null
    ): Comment =
        Comment(
            id = id,
            pageId = pageId,
            authorId = authorId,
            body = body,
            createdAt = createdAt,
            deletedAt = deletedAt
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
}
