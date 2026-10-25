package no.cas.allocator.db

import no.cas.allocator.domain.Call
import no.cas.allocator.domain.CallStatus

data class CallEntity(
    val id: String,
    val region: String,
    val nodeId: String?,
    val status: CallStatus
)

fun Call.toEntity(): CallEntity = CallEntity(id, region, nodeId, status)

fun CallEntity.toDomain(): Call = Call(id, region, nodeId, status)
