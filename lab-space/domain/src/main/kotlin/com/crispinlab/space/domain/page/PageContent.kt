package com.crispinlab.space.domain.page

import com.crispinlab.space.domain.page.ExtractedWikiLink.External
import com.crispinlab.space.domain.page.ExtractedWikiLink.Internal
import java.net.URI

data class PageContent(
    val raw: String
) {
    init {
        require(raw.isNotBlank()) {
            "본문을 입력해 주세요."
        }
        require(raw.length <= MAX_RAW_LENGTH) {
            "본문은 ${MAX_RAW_LENGTH}자를 넘을 수 없습니다."
        }
    }

    fun extractLinks(): List<ExtractedWikiLink> =
        WIKI_LINK_REGEX
            .findAll(raw)
            .mapNotNull { it.toExtracted() }
            .toList()

    private fun MatchResult.toExtracted(): ExtractedWikiLink? {
        val pageIdToken: String? = groups[GROUP_INTERNAL_PAGE_ID]?.value
        val urlToken: String? = groups[GROUP_EXTERNAL_URL]?.value
        return when {
            pageIdToken != null -> {
                pageIdToken
                    .toLongOrNull()
                    ?.let { PageId(it) }
                    ?.let {
                        Internal(
                            targetPageId = it,
                            displayText = groups[GROUP_INTERNAL_DISPLAY]?.value
                        )
                    }
            }

            urlToken != null -> {
                runCatching { URI.create(urlToken) }
                    .getOrNull()
                    ?.let {
                        External(
                            url = it,
                            displayText = groups[GROUP_EXTERNAL_DISPLAY]?.value
                        )
                    }
            }

            else -> {
                null
            }
        }
    }

    companion object {
        const val MAX_RAW_LENGTH: Int = 100_000

        const val GROUP_INTERNAL_PAGE_ID: String = "pageId"
        const val GROUP_INTERNAL_DISPLAY: String = "pageIdText"
        const val GROUP_EXTERNAL_URL: String = "url"
        const val GROUP_EXTERNAL_DISPLAY: String = "urlText"

        val WIKI_LINK_REGEX: Regex =
            Regex(
                """\[\[(?:pageId:(?<$GROUP_INTERNAL_PAGE_ID>\d+)(?:\|""" +
                    """(?<$GROUP_INTERNAL_DISPLAY>[^\[\]|]+))?|""" +
                    """(?<$GROUP_EXTERNAL_URL>https?://[^\s\[\]|]+)""" +
                    """(?:\|(?<$GROUP_EXTERNAL_DISPLAY>[^\[\]|]+))?)\]\]"""
            )
    }
}
