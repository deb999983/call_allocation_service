package no.cas.allocator.db

import no.cas.allocator.domain.CallNode

data class NodeKey(val id: String, val region: String)

object NodeStore {
    private val nodes: MutableMap<NodeKey, CallNodeEntity> = mutableMapOf()

    @Synchronized
    fun <T> withLock(block: () -> T) = block()

    @Synchronized
    fun saveNode(node: CallNode): CallNode {
        val entity = node.toEntity()
        nodes[NodeKey(node.id, node.region)] = entity
        return entity.toDomain()
    }

    @Synchronized
    fun getNode(id: String, region: String): CallNode? {
        return nodes[NodeKey(id, region)]?.toDomain()
    }
}