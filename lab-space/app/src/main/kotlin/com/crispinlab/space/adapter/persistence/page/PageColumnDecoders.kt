package com.crispinlab.space.adapter.persistence.page

import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.page.Visibility.Companion.asVisibility

internal fun decodeVisibility(stored: String): Visibility =
    runCatching { stored.asVisibility() }
        .getOrElse { cause ->
            throw IllegalStateException("저장된 visibility 값을 해석할 수 없습니다.", cause)
        }
