package com.crispinlab.space.application.usecase.page

import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.application.port.outgoing.page.PageVisibilityRecord
import com.crispinlab.space.domain.page.PageContent
import com.crispinlab.space.domain.page.PageId
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode

fun PageContent.maskPageLinksBy(
    mapper: ObjectMapper,
    scope: VisibilityScope,
    visibilityLookup: (Set<PageId>) -> Map<PageId, PageVisibilityRecord>
): PageContent {
    val root = parseOrEmpty(mapper, raw)
    val nodes: List<ObjectNode> = root.collectPageLinkNodes()
    if (nodes.isEmpty()) return this
    val ids: Set<PageId> = nodes.mapNotNull { it.attrsPageId() }.toSet()
    if (ids.isEmpty()) return this
    val visibilities: Map<PageId, PageVisibilityRecord> = visibilityLookup(ids)
    nodes.forEach { it.maskIfDeniedBy(scope, visibilities) }
    return PageContent(mapper.writeValueAsString(root))
}

object PageLinkMaskingPolicy {
    const val MASKED_DISPLAY_TEXT: String = "비공개 페이지"
}

private fun ObjectNode.maskIfDeniedBy(
    scope: VisibilityScope,
    visibilities: Map<PageId, PageVisibilityRecord>
) {
    val attrs: ObjectNode = (this["attrs"] as? ObjectNode) ?: putObject("attrs")
    val allowed: Boolean =
        attrsPageId()
            ?.let { visibilities[it] }
            ?.let { scope.allows(it.visibility, it.spaceId, it.authorId) }
            ?: false
    if (!allowed) {
        attrs.put("displayText", PageLinkMaskingPolicy.MASKED_DISPLAY_TEXT)
    }
}
