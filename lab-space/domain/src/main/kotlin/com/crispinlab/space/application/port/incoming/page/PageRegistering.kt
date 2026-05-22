package com.crispinlab.space.application.port.incoming.page

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.page.PageRegistering.Request
import com.crispinlab.space.application.port.incoming.page.PageRegistering.Result
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageId.Companion.asPageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.page.Visibility.Companion.asVisibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceId.Companion.asSpaceId
import com.crispinlab.user.domain.user.AuthContext

interface PageRegistering : UseCase<Request, Result> {
    class Request(
        spaceId: String,
        parentPageId: String? = null,
        val title: String,
        val content: String,
        visibility: String,
        val auth: AuthContext.Authenticated
    ) {
        val spaceId: SpaceId = spaceId.asSpaceId()
        val parentPageId: PageId? = parentPageId?.asPageId()
        val visibility: Visibility = visibility.asVisibility()
    }

    data class Result(
        val pageId: PageId
    )
}
