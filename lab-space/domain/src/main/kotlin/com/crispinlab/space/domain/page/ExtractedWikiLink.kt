package com.crispinlab.space.domain.page

import java.net.URI

sealed interface ExtractedWikiLink {
    val displayText: String?

    data class Internal(
        val targetPageId: PageId,
        override val displayText: String?
    ) : ExtractedWikiLink

    data class External(
        val url: URI,
        override val displayText: String?
    ) : ExtractedWikiLink

    fun toTarget(): PageLink.Target =
        when (this) {
            is Internal -> PageLink.Target.Internal(targetPageId)
            is External -> PageLink.Target.External(url)
        }
}
