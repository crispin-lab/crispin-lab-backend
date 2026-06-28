package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.page.PageGetting
import com.crispinlab.space.application.port.incoming.page.PageGetting.Request
import com.crispinlab.space.application.port.incoming.page.PageGetting.Result
import com.crispinlab.space.application.port.outgoing.page.PageAncestorPort
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.access.canEdit
import com.crispinlab.space.application.usecase.access.handleOrEmpty
import com.crispinlab.space.application.usecase.access.memberSpaceIdsOf
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageContent
import com.crispinlab.space.domain.page.PageErrorCode
import com.crispinlab.user.application.port.outgoing.user.UserHandleQuery
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service

@Service
class PageGettingUseCase(
    private val pageRepository: PageRepository,
    private val pageAncestorPort: PageAncestorPort,
    private val spaceRepository: SpaceRepository,
    private val spaceMemberRepository: SpaceMemberRepository,
    private val userHandleQuery: UserHandleQuery,
    private val transactionProvider: TransactionProvider,
    private val objectMapper: ObjectMapper
) : PageGetting {
    override fun perform(request: Request): Result =
        transactionProvider.transactional(readOnly = true) {
            request
                .toScope()
                .let { scope ->
                    request
                        .toEntity(scope)
                        .let { page ->
                            page.toResult(
                                scope = scope,
                                canEdit =
                                    spaceMemberRepository.canEdit(
                                        viewer = request.viewer,
                                        authorId = page.authorId,
                                        spaceId = page.spaceId
                                    )
                            )
                        }
                }
        }

    private fun Request.toScope(): VisibilityScope =
        VisibilityScope.of(viewer, spaceMemberRepository.memberSpaceIdsOf(viewer))

    private fun Request.toEntity(scope: VisibilityScope): Page {
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

    private fun Page.ancestorsVisibleTo(scope: VisibilityScope): List<Result.AncestorSummary> =
        pageAncestorPort
            .findAncestorsOf(id)
            .filter {
                scope.allows(it.visibility, it.spaceVisibility, it.spaceId, it.authorId)
            }.map {
                Result.AncestorSummary(
                    pageId = it.pageId,
                    title = it.title
                )
            }

    private fun Page.maskedContent(scope: VisibilityScope): PageContent =
        content.maskPageLinksBy(
            mapper = objectMapper,
            scope = scope,
            visibilityLookup = pageRepository::findVisibilitiesByIds
        )

    private fun Page.toResult(
        scope: VisibilityScope,
        canEdit: Boolean
    ): Result =
        Result(
            pageId = id,
            spaceId = spaceId,
            parentPageId = parentPageId,
            authorId = authorId,
            authorHandle = userHandleQuery.handleOrEmpty(authorId),
            title = title,
            content = maskedContent(scope).raw,
            visibility = visibility,
            currentVersion = currentVersion,
            displayOrder = displayOrder,
            canEdit = canEdit,
            createdAt = createdAt,
            updatedAt = updatedAt,
            ancestors = ancestorsVisibleTo(scope)
        )
}
