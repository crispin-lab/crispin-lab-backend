package com.crispinlab.user.adapter.persistence.user

import com.crispinlab.common.persistence.ExposedEntityRepository
import com.crispinlab.user.application.port.outgoing.user.UserRepository
import com.crispinlab.user.domain.user.EmailAddress
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.SystemRole
import com.crispinlab.user.domain.user.User
import com.crispinlab.user.domain.user.UserId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpsertStatement
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

@Repository
class ExposedUserRepository :
    ExposedEntityRepository<User, UserId>(),
    UserRepository {
    override val table = Users
    override val idColumn = Users.id
    override val deletedAtColumn = Users.deletedAt
    override val updateExclude = listOf(Users.id, Users.createdAt, Users.deletedAt)

    override fun ResultRow.toEntity(): User =
        User(
            id = UserId(this[Users.id]),
            email = EmailAddress(this[Users.email]),
            handle = Handle(this[Users.handle]),
            role = decodeRole(this[Users.role]),
            createdAt = this[Users.createdAt],
            updatedAt = this[Users.updatedAt],
            deletedAt = this[Users.deletedAt]
        )

    override fun upsertBody(
        builder: UpsertStatement<Long>,
        entity: User
    ) {
        builder[Users.id] = entity.id.value
        builder[Users.email] = entity.email.value
        builder[Users.handle] = entity.handle.value
        builder[Users.role] = entity.role.name
        builder[Users.createdAt] = entity.createdAt
        builder[Users.updatedAt] = entity.updatedAt
        builder[Users.deletedAt] = entity.deletedAt
    }

    @Suppress("RedundantOverride")
    override fun delete(id: UserId) = super.delete(id)

    override fun findByEmail(email: EmailAddress): User? =
        Users
            .selectAll()
            .where { (Users.email eq email.value) and notDeleted() }
            .firstOrNull()
            ?.toEntity()

    override fun existsByEmail(email: EmailAddress): Boolean =
        Users
            .select(Users.id)
            .where { (Users.email eq email.value) and notDeleted() }
            .limit(1)
            .empty()
            .not()

    override fun existsByHandle(handle: Handle): Boolean =
        Users
            .select(Users.id)
            .where { (Users.handle eq handle.value) and notDeleted() }
            .limit(1)
            .empty()
            .not()

    private fun decodeRole(stored: String): SystemRole =
        runCatching { SystemRole.valueOf(stored) }
            .getOrElse { cause ->
                throw IllegalStateException("저장된 role 값을 해석할 수 없습니다.", cause)
            }
}
