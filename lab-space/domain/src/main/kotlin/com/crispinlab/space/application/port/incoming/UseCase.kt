package com.crispinlab.space.application.port.incoming

interface UseCase<Request, Result> {
    fun perform(request: Request): Result
}
