package no.cas.allocator.db

import no.cas.allocator.domain.CallNode

data class CallNodeEntity(
    val id: String,
    val region: String,
    val capacity: Int,
    val calls: List<CallEntity>,
    val reportedCalls: Int,
    val callsSinceReport: Int
) {
    fun toDomain(): CallNode {
        return CallNode.restore(id, region, capacity, calls.map { it.toDomain() }, reportedCalls, callsSinceReport)
    }
}

fun CallNode.toEntity(): CallNodeEntity =
    CallNodeEntity(id, region, capacity, calls.map { it.toEntity() }, reportedCalls, callsSinceReport)

