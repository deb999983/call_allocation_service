package no.cas.allocator.services

import no.cas.allocator.db.NodeStore
import no.cas.allocator.domain.CallNode
import no.cas.allocator.exceptions.NodeDoesNotExistException

class NodeService(private val nodeStore: NodeStore) {
    fun registerNode(nodeId: String, region: String, capacity: Int, currentCalls: Int): CallNode {
        return nodeStore.withLock {
            val node = nodeStore.getNode(nodeId, region) ?: CallNode(nodeId, region, capacity)
            node.newReport(capacity, currentCalls)
            nodeStore.saveNode(node)
        }
    }

    fun getNode(nodeId: String, region: String): CallNode {
        return nodeStore.withLock {
            nodeStore.getNode(nodeId, region)?: throw NodeDoesNotExistException(nodeId, region)
        }
    }
}