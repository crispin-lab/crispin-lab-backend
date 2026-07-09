package com.crispinlab.space.application.usecase.audit

import com.crispinlab.common.id.IdGenerator
import com.crispinlab.space.application.port.outgoing.audit.SpaceAuditRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.audit.AuditChangeSummary
import com.crispinlab.space.domain.audit.SpaceAuditAction
import com.crispinlab.space.domain.audit.SpaceAuditEntry
import com.crispinlab.space.domain.audit.SpaceAuditEntryId
import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceSnapshot
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

@Component
class SpaceAuditRecorder(
    private val spaceAuditRepository: SpaceAuditRepository,
    private val idGenerator: IdGenerator,
    private val objectMapper: ObjectMapper
) {
    fun recordRegistered(
        space: Space,
        viewer: Viewer.Member
    ) {
        record(
            spaceId = space.id,
            actor = viewer,
            action = SpaceAuditAction.REGISTERED,
            summaryJson = snapshotJson(SpaceSnapshot.of(space))
        )
    }

    fun recordEdited(
        spaceId: SpaceId,
        before: SpaceSnapshot,
        after: Space,
        viewer: Viewer.Member
    ) {
        val diffs = buildDiff(before, SpaceSnapshot.of(after))
        if (diffs.isEmpty()) return
        record(
            spaceId = spaceId,
            actor = viewer,
            action = SpaceAuditAction.EDITED,
            summaryJson = objectMapper.writeValueAsString(diffs)
        )
    }

    fun recordDeleted(
        spaceId: SpaceId,
        snapshot: SpaceSnapshot,
        viewer: Viewer.Member
    ) {
        record(
            spaceId = spaceId,
            actor = viewer,
            action = SpaceAuditAction.DELETED,
            summaryJson = snapshotJson(snapshot)
        )
    }

    private fun record(
        spaceId: SpaceId,
        actor: Viewer.Member,
        action: SpaceAuditAction,
        summaryJson: String
    ) {
        spaceAuditRepository.save(
            SpaceAuditEntry(
                id = SpaceAuditEntryId(idGenerator.next()),
                spaceId = spaceId,
                actorUserId = actor.userId,
                action = action,
                changeSummary = AuditChangeSummary(summaryJson)
            )
        )
    }

    private fun snapshotJson(snapshot: SpaceSnapshot): String =
        objectMapper.writeValueAsString(
            mapOf(
                FIELD_NAME to snapshot.name,
                FIELD_DESCRIPTION to snapshot.description,
                FIELD_VISIBILITY to snapshot.visibility.name
            )
        )

    private fun buildDiff(
        before: SpaceSnapshot,
        after: SpaceSnapshot
    ): Map<String, Map<String, String?>> {
        val diffs = mutableMapOf<String, Map<String, String?>>()
        if (before.name != after.name) {
            diffs[FIELD_NAME] = diffEntry(before.name, after.name)
        }
        if (before.description != after.description) {
            diffs[FIELD_DESCRIPTION] = diffEntry(before.description, after.description)
        }
        if (before.visibility != after.visibility) {
            diffs[FIELD_VISIBILITY] = diffEntry(before.visibility.name, after.visibility.name)
        }
        return diffs
    }

    private fun diffEntry(
        before: String,
        after: String
    ): Map<String, String?> = mapOf(KEY_BEFORE to before, KEY_AFTER to after)

    companion object {
        private const val FIELD_NAME = "name"
        private const val FIELD_DESCRIPTION = "description"
        private const val FIELD_VISIBILITY = "visibility"
        private const val KEY_BEFORE = "before"
        private const val KEY_AFTER = "after"
    }
}
