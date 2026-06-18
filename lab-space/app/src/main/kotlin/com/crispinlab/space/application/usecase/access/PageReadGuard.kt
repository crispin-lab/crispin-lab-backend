package com.crispinlab.space.application.usecase.access

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageErrorCode
import com.crispinlab.space.domain.page.PageId

internal fun requireReadablePage(
    pageRepository: PageRepository,
    spaceRepository: SpaceRepository,
    spaceMemberRepository: SpaceMemberRepository,
    viewer: Viewer,
    pageId: PageId
): Page =
    requireReadablePage(
        pageRepository,
        spaceRepository,
        VisibilityScope.of(viewer, spaceMemberRepository.memberSpaceIdsOf(viewer)),
        pageId
    )

internal fun requireReadablePage(
    pageRepository: PageRepository,
    spaceRepository: SpaceRepository,
    scope: VisibilityScope,
    pageId: PageId
): Page {
    val page =
        pageRepository.findBy(pageId)
            ?: throw NotFoundException(PageErrorCode.PAGE_NOT_FOUND)
    val spaceVisibility =
        spaceRepository.findVisibility(page.spaceId)
            ?: throw NotFoundException(PageErrorCode.PAGE_NOT_FOUND)
    return page.takeIf {
        scope.allows(it.visibility, spaceVisibility, it.spaceId, it.authorId)
    } ?: throw NotFoundException(PageErrorCode.PAGE_NOT_FOUND)
}
