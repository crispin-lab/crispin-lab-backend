package com.crispinlab.user.adapter.persistence.credential

import com.crispinlab.common.persistence.ExposedEntityRepository
import com.crispinlab.user.adapter.persistence.user.Users
import com.crispinlab.user.application.port.outgoing.credential.UserCredentialRepository
import com.crispinlab.user.domain.credential.Credential
import com.crispinlab.user.domain.credential.OAuthProvider
import com.crispinlab.user.domain.credential.PasswordHash
import com.crispinlab.user.domain.credential.UserCredential
import com.crispinlab.user.domain.credential.UserCredentialId
import com.crispinlab.user.domain.user.UserId
import java.time.Instant
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Join
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.statements.UpsertStatement
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

@Repository
class ExposedUserCredentialRepository :
    ExposedEntityRepository<UserCredential, UserCredentialId>(),
    UserCredentialRepository {
    override val table = UserCredentials
    override val idColumn = UserCredentials.id
    override val deletedAtColumn: Column<Instant?>? = null
    override val updateExclude =
        listOf(
            UserCredentials.id,
            UserCredentials.userId,
            UserCredentials.createdAt,
            UserCredentials.type
        )

    override fun ResultRow.toEntity(): UserCredential =
        UserCredential(
            id = UserCredentialId(this[UserCredentials.id]),
            userId = UserId(this[UserCredentials.userId]),
            credential = decodeCredential(this),
            createdAt = this[UserCredentials.createdAt],
            updatedAt = this[UserCredentials.updatedAt]
        )

    override fun upsertBody(
        builder: UpsertStatement<Long>,
        entity: UserCredential
    ) {
        builder[UserCredentials.id] = entity.id.value
        builder[UserCredentials.userId] = entity.userId.value
        builder[UserCredentials.createdAt] = entity.createdAt
        builder[UserCredentials.updatedAt] = entity.updatedAt
        when (val credential = entity.credential) {
            is Credential.Password -> {
                builder[UserCredentials.type] = TYPE_PASSWORD
                builder[UserCredentials.passwordHash] = credential.hash.value
                builder[UserCredentials.oauthProvider] = null
                builder[UserCredentials.oauthSubjectId] = null
            }

            is Credential.OAuth -> {
                builder[UserCredentials.type] = TYPE_OAUTH
                builder[UserCredentials.passwordHash] = null
                builder[UserCredentials.oauthProvider] = credential.provider.name
                builder[UserCredentials.oauthSubjectId] = credential.subjectId
            }
        }
    }

    @Suppress("RedundantOverride")
    override fun delete(id: UserCredentialId) = super.delete(id)

    // users 와 inner join 해서 soft deleted 사용자의 자격증명은 노출하지 않는다.
    // user_credentials 자체는 hard delete + FK CASCADE 미부착이라 user 가 soft delete 돼도
    // credential row 가 남기 때문에 join 단계의 명시 필터가 필요하다.
    // schema 에 FK 가 없어 infix `innerJoin` 이 매칭 키를 추론하지 못하므로 Join 을 직접 구성한다.
    override fun findPasswordBy(userId: UserId): UserCredential? =
        Join(
            table = UserCredentials,
            otherTable = Users,
            joinType = JoinType.INNER,
            onColumn = UserCredentials.userId,
            otherColumn = Users.id
        ).select(UserCredentials.columns)
            .where {
                (UserCredentials.userId eq userId.value) and
                    (UserCredentials.type eq TYPE_PASSWORD) and
                    Users.deletedAt.isNull()
            }.firstOrNull()
            ?.toEntity()

    private fun decodeCredential(row: ResultRow): Credential =
        when (val type = row[UserCredentials.type]) {
            TYPE_PASSWORD -> {
                Credential.Password(
                    hash =
                        PasswordHash(
                            row[UserCredentials.passwordHash]
                                ?: throw IllegalStateException(
                                    "저장된 PASSWORD 자격증명에 비밀번호 해시가 없습니다."
                                )
                        )
                )
            }

            TYPE_OAUTH -> {
                Credential.OAuth(
                    provider =
                        decodeProvider(
                            row[UserCredentials.oauthProvider]
                                ?: throw IllegalStateException(
                                    "저장된 OAUTH 자격증명에 provider 가 없습니다."
                                )
                        ),
                    subjectId =
                        row[UserCredentials.oauthSubjectId]
                            ?: throw IllegalStateException(
                                "저장된 OAUTH 자격증명에 subject ID 가 없습니다."
                            )
                )
            }

            else -> {
                throw IllegalStateException("알 수 없는 자격증명 type 입니다: $type")
            }
        }

    private fun decodeProvider(stored: String): OAuthProvider =
        runCatching { OAuthProvider.valueOf(stored) }
            .getOrElse { cause ->
                throw IllegalStateException(
                    "저장된 OAuth provider 값을 해석할 수 없습니다.",
                    cause
                )
            }

    companion object {
        // V*__lab_user_init.sql 의 partial unique (`WHERE type = 'PASSWORD'`) 와 동일해야 한다.
        // 마이그레이션이 forward-only 라 코드에서 import 불가 — 변경 시 두 곳 모두 갱신할 것.
        private const val TYPE_PASSWORD = "PASSWORD"
        private const val TYPE_OAUTH = "OAUTH"
    }
}
