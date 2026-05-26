package com.crispinlab.space.application.usecase.access

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageErrorCode
import com.crispinlab.space.domain.page.PageId

internal fun PageRepository.findReadableBy(
    viewer: Viewer,
    pageId: PageId,
    spaceMemberRepository: SpaceMemberRepository
): Page {
    val scope = VisibilityScope.of(viewer, spaceMemberRepository.lookupMemberSpaceIds(viewer))
    return findBy(pageId)
        ?.takeIf { scope.allows(it.visibility, it.spaceId, it.authorId) }
        ?: throw NotFoundException(PageErrorCode.PAGE_NOT_FOUND)
}
