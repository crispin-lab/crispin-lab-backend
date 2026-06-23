package com.crispinlab.user.application.credential

import com.crispinlab.common.exception.DomainException
import com.crispinlab.common.exception.ErrorCode

class PasswordPolicyException(
    errorCode: ErrorCode
) : DomainException(errorCode)
