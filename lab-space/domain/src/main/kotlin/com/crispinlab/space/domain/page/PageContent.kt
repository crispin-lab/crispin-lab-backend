package com.crispinlab.space.domain.page

data class PageContent(
    val raw: String
) {
    init {
        require(raw.isNotBlank()) {
            "본문을 입력해 주세요."
        }
    }

    fun extractLinks(): List<ExtractedWikiLink> =
        WIKI_LINK_REGEX
            .findAll(raw)
            .mapNotNull { match ->
                match.groupValues[1]
                    .substringBefore('|')
                    .trim()
                    .takeIf { it.isNotBlank() }
                    ?.let { ExtractedWikiLink(target = it, type = classifyType(it)) }
            }.toList()

    private fun classifyType(target: String): PageLink.Type =
        if (
            target.startsWith("http://", ignoreCase = true) ||
            target.startsWith("https://", ignoreCase = true)
        ) {
            PageLink.Type.EXTERNAL
        } else {
            PageLink.Type.INTERNAL
        }

    companion object {
        private val WIKI_LINK_REGEX: Regex = Regex("""\[\[([^\[\]]+)\]\]""")
    }
}
