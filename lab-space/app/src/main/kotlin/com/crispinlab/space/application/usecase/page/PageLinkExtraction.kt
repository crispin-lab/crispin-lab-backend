package com.crispinlab.space.application.usecase.page

import com.crispinlab.space.domain.page.ExtractedPageLink
import com.crispinlab.space.domain.page.PageContent
import com.crispinlab.space.domain.page.PageId
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode

fun PageContent.extractPageLinks(mapper: ObjectMapper): List<ExtractedPageLink> =
    parseOrEmpty(mapper, raw)
        .collectPageLinkNodes()
        .mapNotNull { it.toExtracted() }

internal fun parseOrEmpty(
    mapper: ObjectMapper,
    raw: String
): JsonNode =
    runCatching { mapper.readTree(raw) }
        .getOrNull()
        ?: mapper.createObjectNode()

internal fun JsonNode.collectPageLinkNodes(): List<ObjectNode> {
    val collected: MutableList<ObjectNode> = mutableListOf()
    walkInto(collected)
    return collected
}

private fun JsonNode.walkInto(collected: MutableList<ObjectNode>) {
    if (this is ObjectNode && this["type"]?.asText() == "pageLink") {
        collected += this
    }
    elements().forEachRemaining { it.walkInto(collected) }
}

internal fun ObjectNode.attrsPageId(): PageId? =
    this["attrs"]
        ?.get("pageId")
        ?.asText()
        ?.toLongOrNull()
        ?.let(::PageId)

private fun ObjectNode.toExtracted(): ExtractedPageLink? {
    val targetPageId: PageId = attrsPageId() ?: return null
    val displayText: String? =
        this["attrs"]?.get("displayText")?.takeIf { it.isTextual }?.asText()
    return ExtractedPageLink(
        targetPageId = targetPageId,
        displayText = displayText
    )
}
