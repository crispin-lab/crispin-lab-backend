package com.crispinlab.common.domain

interface Entity<ID : EntityId> {
    val id: ID
}
