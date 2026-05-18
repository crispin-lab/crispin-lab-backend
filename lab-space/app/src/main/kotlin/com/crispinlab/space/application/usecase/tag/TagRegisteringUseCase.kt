package com.crispinlab.space.application.usecase.tag

import com.crispinlab.common.exception.ConflictException
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.id.IdGenerator
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.tag.TagRegistering
import com.crispinlab.space.application.port.incoming.tag.TagRegistering.Request
import com.crispinlab.space.application.port.incoming.tag.TagRegistering.Result
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.tag.TagRepository
import com.crispinlab.space.domain.space.SpaceErrorCode
import com.crispinlab.space.domain.tag.Tag
import com.crispinlab.space.domain.tag.TagErrorCode
import com.crispinlab.space.domain.tag.TagId
import java.time.Instant.now
import org.springframework.stereotype.Service

@Service
class TagRegisteringUseCase(
    private val tagRepository: TagRepository,
    private val spaceRepository: SpaceRepository,
    private val idGenerator: IdGenerator,
    private val transactionProvider: TransactionProvider
) : TagRegistering {
    override fun perform(request: Request): Result =
        transactionProvider.transactional {
            request
                .also {
                    it.validate()
                }.toEntity()
                .let {
                    tagRepository.save(it)
                }.toResult()
        }

    // Space 존재(404) → 이름 중복(409) 순. 입력 유효성을 먼저 보고 도메인 충돌은 그 다음 — 둘 다 false 일 때
    // 응답이 404 로 결정되는 게 의도다 (없는 space 의 중복 이름은 의미가 없으므로).
    private fun Request.validate() {
        spaceRepository.findBy(spaceId)
            ?: throw NotFoundException(SpaceErrorCode.SPACE_NOT_FOUND)
        if (tagRepository.existsByNameAndSpaceId(spaceId, name)) {
            throw ConflictException(TagErrorCode.TAG_NAME_DUPLICATED)
        }
    }

    private fun Request.toEntity(): Tag =
        Tag(
            id = TagId(idGenerator.next()),
            spaceId = spaceId,
            name = name,
            createdAt = now()
        )

    private fun Tag.toResult(): Result = Result(tagId = id)
}
