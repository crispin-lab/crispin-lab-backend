package com.crispinlab.space.application.port.incoming.tag

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.tag.TagDeleting.Request
import com.crispinlab.space.domain.tag.TagId
import com.crispinlab.space.domain.tag.TagId.Companion.asTagId
import com.crispinlab.space.domain.user.UserId

interface TagDeleting : UseCase<Request, Unit> {
    class Request(
        tagId: String,
        val currentUserId: UserId
    ) {
        val tagId: TagId = tagId.asTagId()
    }
}
