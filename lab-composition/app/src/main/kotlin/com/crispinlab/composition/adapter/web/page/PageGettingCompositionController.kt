package com.crispinlab.composition.adapter.web.page

import com.crispinlab.composition.application.port.outgoing.user.UserHandleLookup
import com.crispinlab.composition.application.port.outgoing.user.handleOf
import com.crispinlab.space.adapter.web.auth.toViewer
import com.crispinlab.space.application.port.incoming.page.PageGetting
import com.crispinlab.space.application.port.incoming.page.PageGetting.Request
import com.crispinlab.space.application.port.incoming.page.PageGetting.Result
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.adapter.web.auth.Auth
import com.crispinlab.user.domain.user.UserId
import java.time.Instant
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/pages/{pageId}")
class PageGettingCompositionController(
    private val useCase: PageGetting,
    private val userHandleLookup: UserHandleLookup
) {
    @GetMapping
    fun get(
        @PathVariable pageId: String,
        auth: Auth?
    ): PagePayload =
        Request(
            pageId = pageId,
            viewer = auth.toViewer()
        ).let {
            useCase.perform(it)
        }.toPayload()

    private fun Result.toPayload(): PagePayload =
        PagePayload(
            pageId = pageId,
            spaceId = spaceId,
            parentPageId = parentPageId,
            authorId = authorId,
            authorHandle = userHandleLookup.handleOf(authorId),
            title = title,
            content = content,
            visibility = visibility,
            currentVersion = currentVersion,
            displayOrder = displayOrder,
            canEdit = canEdit,
            canComment = canComment,
            createdAt = createdAt,
            updatedAt = updatedAt,
            ancestors =
                ancestors.map {
                    AncestorSummary(pageId = it.pageId, title = it.title)
                }
        )

    data class PagePayload(
        val pageId: PageId,
        val spaceId: SpaceId,
        val parentPageId: PageId?,
        val authorId: UserId,
        val authorHandle: String,
        val title: String,
        val content: String,
        val visibility: Visibility,
        val currentVersion: Int,
        val displayOrder: Int,
        val canEdit: Boolean,
        val canComment: Boolean,
        val createdAt: Instant,
        val updatedAt: Instant,
        val ancestors: List<AncestorSummary>
    )

    data class AncestorSummary(
        val pageId: PageId,
        val title: String
    )
}
