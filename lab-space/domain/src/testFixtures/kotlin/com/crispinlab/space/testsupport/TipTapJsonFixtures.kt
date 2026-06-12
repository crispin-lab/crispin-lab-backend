package com.crispinlab.space.testsupport

object TipTapJsonFixtures {
    const val EMPTY_DOC: String = """{"type":"doc","content":[]}"""

    fun doc(vararg nodes: String): String =
        """{"type":"doc","content":[${nodes.joinToString(",")}]}"""

    fun paragraph(vararg inlines: String): String =
        """{"type":"paragraph","content":[${inlines.joinToString(",")}]}"""

    fun bulletList(vararg items: String): String =
        """{"type":"bulletList","content":[${items.joinToString(",")}]}"""

    fun listItem(vararg blocks: String): String =
        """{"type":"listItem","content":[${blocks.joinToString(",")}]}"""

    fun text(text: String): String = """{"type":"text","text":"${text.escape()}"}"""

    fun pageLink(
        pageId: Long,
        displayText: String?
    ): String {
        val attrs: String =
            if (displayText == null) {
                """"pageId":"$pageId","displayText":null"""
            } else {
                """"pageId":"$pageId","displayText":"${displayText.escape()}""""
            }
        return """{"type":"pageLink","attrs":{$attrs}}"""
    }

    fun pageLinkWithRawAttrs(rawAttrs: String): String =
        """{"type":"pageLink","attrs":{$rawAttrs}}"""

    private fun String.escape(): String =
        buildString(length) {
            for (c in this@escape) {
                when (c) {
                    '\\' -> {
                        append("\\\\")
                    }

                    '"' -> {
                        append("\\\"")
                    }

                    '\n' -> {
                        append("\\n")
                    }

                    '\r' -> {
                        append("\\r")
                    }

                    '\t' -> {
                        append("\\t")
                    }

                    '\b' -> {
                        append("\\b")
                    }

                    '' -> {
                        append("\\f")
                    }

                    else -> {
                        if (c.code < 0x20) {
                            append("\\u%04x".format(c.code))
                        } else {
                            append(c)
                        }
                    }
                }
            }
        }
}
