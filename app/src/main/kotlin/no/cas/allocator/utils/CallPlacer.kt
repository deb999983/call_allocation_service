package no.cas.allocator.utils

import no.cas.allocator.db.NodeStore
import no.cas.allocator.domain.Call
import no.cas.allocator.domain.CallNode
import no.cas.allocator.domain.CallStatus
import no.cas.allocator.exceptions.AllNodesFullException
import no.cas.allocator.exceptions.CallAlreadyFinishedException
import no.cas.allocator.exceptions.CallIdConflictException
import no.cas.allocator.exceptions.NodeDoesNotExistInRegionException

class CallPlacer(private val nodeStore: NodeStore) {

    fun place(call: Call): CallNode = nodeStore.withLock {
        val nodesInRegion = nodeStore.findNodesInRegion(call.region)
        if (nodesInRegion.isEmpty()) {
            throw NodeDoesNotExistInRegionException(call.region)
        }

        val allocatedNode = allocateIfRunningCall(call)
        if (allocatedNode != null) {
            return@withLock allocatedNode
        }

        val allocatableNodes = nodesInRegion.filter { node -> node.canAllocateNewCalls() }
        if (allocatableNodes.isEmpty()) {
            throw AllNodesFullException(call.id, call.region)
        }

        val node = pickNode(allocatableNodes)
        node.addCall(call.id)
        nodeStore.saveNode(node)
    }

    // Try to assign the call to the node with minimum load factor.
    private fun pickNode(allocatableNodes: List<CallNode>): CallNode {
        return allocatableNodes.minBy { it.loadFactor() }
    }

    private fun allocateIfRunningCall(call: Call): CallNode? {
        val allocatedNode = nodeStore.findNodeWithCall(call.id) ?: return null // Is new
        if (allocatedNode.region != call.region) {
            throw CallIdConflictException(call.id)
        }

        if (allocatedNode.getCall(call.id)?.status == CallStatus.FINISHED) {
            throw CallAlreadyFinishedException(call.id)
        }
        return allocatedNode
    }
}
