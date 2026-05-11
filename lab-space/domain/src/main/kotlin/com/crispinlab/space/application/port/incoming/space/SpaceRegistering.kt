package com.crispinlab.space.application.port.incoming.space

import com.crispinlab.space.application.port.incoming.UseCase
import com.crispinlab.space.application.port.incoming.space.SpaceRegistering.Request
import com.crispinlab.space.application.port.incoming.space.SpaceRegistering.Result

interface SpaceRegistering : UseCase<Request, Result> {
    class Request(
        val name: String,
        val description: String
    )

    data class Result(
        val spaceId: Long
    )
}
