package com.crispinlab.user.domain.credential

@ConsistentCopyVisibility
data class Password private constructor(
    val raw: String
) {
    override fun toString(): String = "Password(raw=***)"

    sealed interface Outcome {
        data class Ok(
            val password: Password
        ) : Outcome

        sealed interface Violation : Outcome {
            val errorCode: PasswordErrorCode
        }

        data object ContainsWhitespace : Violation {
            override val errorCode: PasswordErrorCode =
                PasswordErrorCode.PASSWORD_CONTAINS_WHITESPACE
        }

        data object TooShort : Violation {
            override val errorCode: PasswordErrorCode = PasswordErrorCode.PASSWORD_TOO_SHORT
        }

        data object TooLong : Violation {
            override val errorCode: PasswordErrorCode = PasswordErrorCode.PASSWORD_TOO_LONG
        }

        data object InsufficientVariety : Violation {
            override val errorCode: PasswordErrorCode =
                PasswordErrorCode.PASSWORD_INSUFFICIENT_VARIETY
        }
    }

    companion object {
        const val MIN_LENGTH: Int = 8
        const val MAX_LENGTH: Int = 72
        const val REQUIRED_VARIETY: Int = 2

        fun parse(raw: String): Outcome =
            when {
                raw.isEmpty() || raw.first().isWhitespace() || raw.last().isWhitespace() -> {
                    Outcome.ContainsWhitespace
                }

                raw.length < MIN_LENGTH -> {
                    Outcome.TooShort
                }

                raw.length > MAX_LENGTH -> {
                    Outcome.TooLong
                }

                varietyCount(raw) < REQUIRED_VARIETY -> {
                    Outcome.InsufficientVariety
                }

                else -> {
                    Outcome.Ok(Password(raw))
                }
            }

        private fun varietyCount(raw: String): Int {
            val seen = HashSet<Category>(Category.entries.size)
            for (c in raw) {
                seen.add(c.category())
                if (seen.size == Category.entries.size) return seen.size
            }
            return seen.size
        }

        private fun Char.category(): Category =
            when {
                this in 'a'..'z' || this in 'A'..'Z' -> Category.ASCII_LETTER
                this in '0'..'9' -> Category.ASCII_DIGIT
                else -> Category.OTHER
            }

        private enum class Category {
            ASCII_LETTER,
            ASCII_DIGIT,
            OTHER
        }
    }
}
