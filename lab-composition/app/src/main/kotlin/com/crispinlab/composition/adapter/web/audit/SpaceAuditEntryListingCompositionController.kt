package com.crispinlab.composition.adapter.web.audit

import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.composition.application.port.incoming.audit.SpaceAuditEntryListingComposition
import com.crispinlab.composition.application.port.incoming.audit.SpaceAuditEntryListingComposition.Request
import com.crispinlab.composition.application.port.incoming.audit.SpaceAuditEntryListingComposition.Result
import com.crispinlab.space.adapter.web.auth.toMember
import com.crispinlab.user.adapter.web.auth.Auth
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/spaces/{spaceId}/audit-entries")
class SpaceAuditEntryListingCompositionController(
    private val useCase: SpaceAuditEntryListingComposition
) {
    @GetMapping
    fun list(
        @PathVariable spaceId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "$DEFAULT_SIZE") size: Int,
        auth: Auth
    ): PageResult<Result> =
        Request(
            spaceId = spaceId,
            page = page,
            size = size,
            viewer = auth.toMember()
        ).let { useCase.perform(it) }
}
