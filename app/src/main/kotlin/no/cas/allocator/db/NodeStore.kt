package no.cas.allocator.db

import no.cas.allocator.domain.CallNode
import no.cas.allocator.exceptions.CallIdConflictException

data class NodeKey(val id: String, val region: String)

object NodeStore {
    private val nodes: MutableMap<NodeKey, CallNodeEntity> = mutableMapOf()
    private val callIndex: MutableMap<String, NodeKey> = mutableMapOf()

    @Synchronized
    fun <T> withLock(block: () -> T) = block()

    @Synchronized
    fun saveNode(node: CallNode): CallNode {
        val key = NodeKey(node.id, node.region)
        val entity = node.toEntity()
        ensureUniqueCallIds(key, entity)

        syncCallIndex(key, entity)
        nodes[key] = entity
        return entity.toDomain()
    }

    private fun ensureUniqueCallIds(key: NodeKey, entity: CallNodeEntity) {
        val seen = mutableSetOf<String>()
        entity.calls.forEach { call ->
            val owner = callIndex[call.id]
            if (!seen.add(call.id) || (owner != null && owner != key)) {
                throw CallIdConflictException(call.id)
            }
        }
    }

    // Replaces the index entries of the stored node with those of the node being saved,
    // so calls no longer on the node don't keep pointing at it.
    private fun syncCallIndex(key: NodeKey, entity: CallNodeEntity) {
        nodes[key]?.calls?.forEach { callIndex.remove(it.id) }
        entity.calls.forEach { callIndex[it.id] = key }
    }

    @Synchronized
    fun getNode(id: String, region: String): CallNode? {
        return nodes[NodeKey(id, region)]?.toDomain()
    }
    
    @Synchronized
    fun findNodesInRegion(region: String): List<CallNode> {
        return nodes.entries.filter { it.key.region == region }.map { it.value.toDomain() }
    }
    
    @Synchronized
    fun findNodeWithCall(callId: String): CallNode? {
        return callIndex[callId]?.let { nodes[it] }?.toDomain()
    }
}