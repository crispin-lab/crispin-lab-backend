package com.crispinlab.common.application

interface UseCase<Request, Result> {
    fun perform(request: Request): Result
}
