package com.crispinlab.space.application.usecase.mention

import com.crispinlab.space.domain.comment.CommentContent
import com.crispinlab.space.domain.page.PageContent
import com.crispinlab.user.domain.user.UserId
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode

fun PageContent.extractMentions(mapper: ObjectMapper): List<UserId> =
    extractMentionsFrom(raw, mapper)

fun CommentContent.extractMentions(mapper: ObjectMapper): List<UserId> =
    extractMentionsFrom(raw, mapper)

private fun extractMentionsFrom(
    raw: String,
    mapper: ObjectMapper
): List<UserId> =
    parseOrEmpty(mapper, raw)
        .collectMentionNodes()
        .mapNotNull { it.toMentionedUserId() }
        .distinctBy { it.value }

private fun parseOrEmpty(
    mapper: ObjectMapper,
    raw: String
): JsonNode =
    runCatching { mapper.readTree(raw) }
        .getOrNull()
        ?: mapper.createObjectNode()

private fun JsonNode.collectMentionNodes(): List<ObjectNode> {
    val collected: MutableList<ObjectNode> = mutableListOf()
    walkInto(collected)
    return collected
}

private fun JsonNode.walkInto(collected: MutableList<ObjectNode>) {
    if (this is ObjectNode && this["type"]?.asText() == "mention") {
        collected += this
    }
    elements().forEachRemaining { it.walkInto(collected) }
}

private fun ObjectNode.toMentionedUserId(): UserId? =
    this["attrs"]
        ?.get("userId")
        ?.asText()
        ?.toLongOrNull()
        ?.let(::UserId)
