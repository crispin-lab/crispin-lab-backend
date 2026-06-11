package com.crispinlab.space.domain.page

import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.application.port.outgoing.page.PageVisibilityRecord

fun PageContent.maskPageLinks(
    scope: VisibilityScope,
    visibilities: Map<PageId, PageVisibilityRecord>
): PageContent {
    val masked: String =
        PageContent.WIKI_LINK_REGEX.replace(raw) { match ->
            val pageIdToken: String? = match.groups[PageContent.GROUP_INTERNAL_PAGE_ID]?.value
            val urlToken: String? = match.groups[PageContent.GROUP_EXTERNAL_URL]?.value
            when {
                urlToken != null -> match.value
                pageIdToken == null -> PageLinkMaskingPolicy.MASKED_DISPLAY_TEXT
                else -> resolveInternal(pageIdToken, match.value, scope, visibilities)
            }
        }
    return PageContent(masked)
}

private fun resolveInternal(
    pageIdToken: String,
    rawMatch: String,
    scope: VisibilityScope,
    visibilities: Map<PageId, PageVisibilityRecord>
): String {
    val pageId: PageId =
        pageIdToken.toLongOrNull()?.let(::PageId)
            ?: return PageLinkMaskingPolicy.MASKED_DISPLAY_TEXT
    val record: PageVisibilityRecord =
        visibilities[pageId] ?: return PageLinkMaskingPolicy.MASKED_DISPLAY_TEXT
    return if (scope.allows(record.visibility, record.spaceId, record.authorId)) {
        rawMatch
    } else {
        PageLinkMaskingPolicy.MASKED_DISPLAY_TEXT
    }
}

object PageLinkMaskingPolicy {
    const val MASKED_DISPLAY_TEXT: String = "비공개 페이지"
}
