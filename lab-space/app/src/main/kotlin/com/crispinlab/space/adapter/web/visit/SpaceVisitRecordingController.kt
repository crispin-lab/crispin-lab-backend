package com.crispinlab.space.adapter.web.visit

import com.crispinlab.space.adapter.web.auth.toMember
import com.crispinlab.space.application.port.incoming.visit.SpaceVisitRecording
import com.crispinlab.space.application.port.incoming.visit.SpaceVisitRecording.Request
import com.crispinlab.user.adapter.web.auth.Auth
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/spaces/{spaceId}/visits")
class SpaceVisitRecordingController(
    private val useCase: SpaceVisitRecording
) {
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun record(
        @PathVariable spaceId: String,
        auth: Auth
    ) {
        Request(spaceId = spaceId, viewer = auth.toMember())
            .let { useCase.perform(it) }
    }
}
