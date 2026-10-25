package no.cas.allocator.services

import no.cas.allocator.db.NodeStore
import no.cas.allocator.domain.Call
import no.cas.allocator.domain.CallNode
import no.cas.allocator.domain.CallStatus
import no.cas.allocator.exceptions.CallAlreadyFinishedException
import no.cas.allocator.exceptions.CallNotAllocatedToAnyNodeException
import no.cas.allocator.exceptions.CallNotFoundException
import no.cas.allocator.utils.CallPlacer

class CallService(private val nodeStore: NodeStore) {
    private val placer = CallPlacer(nodeStore)

    // Returns the node the call runs on.
    fun allocateCall(callId: String, region: String): CallNode = nodeStore.withLock {
        placer.place(Call(callId, region))
    }


    fun getCall(callId: String): Call = nodeStore.withLock {
        nodeStore.findNodeWithCall(callId)?.getCall(callId)
            ?: throw CallNotFoundException(callId)
    }

    fun dropCall(callId: String): Unit = nodeStore.withLock {
        val node = nodeStore.findNodeWithCall(callId) ?: throw CallNotAllocatedToAnyNodeException(callId)
        rejectIfFinished(callId)
        node.dropCall(callId)
        nodeStore.saveNode(node)
    }

    private fun rejectIfFinished(callId: String) {
        if (nodeStore.findNodeWithCall(callId)?.getCall(callId)?.status == CallStatus.FINISHED) {
            throw CallAlreadyFinishedException(callId)
        }
    }
}
