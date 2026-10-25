package no.cas.allocator.db

import no.cas.allocator.domain.CallNode

data class CallNodeEntity(
    val id: String,
    val region: String,
    val capacity: Int,
    val calls: List<CallEntity>
) {
    fun toDomain(): CallNode {
        return CallNode.restore(id, region, capacity, calls.map { it.toDomain() })
    }
}

fun CallNode.toEntity(): CallNodeEntity = CallNodeEntity(id, region, capacity, calls.map { it.toEntity() })

