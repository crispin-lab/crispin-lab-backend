package com.crispinlab.user.adapter.persistence.user

import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.UserId
import org.slf4j.LoggerFactory

internal object UserHandleDecoder {
    private val log = LoggerFactory.getLogger(javaClass)

    fun decode(
        stored: String,
        userId: UserId
    ): Handle? =
        runCatching { Handle(stored) }
            .onFailure {
                log.warn(
                    "저장된 handle 값을 해석할 수 없습니다 — skip. userId={}",
                    userId.value
                )
            }.getOrNull()
}
