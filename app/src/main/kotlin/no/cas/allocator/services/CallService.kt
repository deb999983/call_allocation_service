package no.cas.allocator.services

import no.cas.allocator.db.NodeStore
import no.cas.allocator.domain.Call
import no.cas.allocator.exceptions.CallNotFoundException
import no.cas.allocator.utils.CallPlacer

class CallService(private val nodeStore: NodeStore) {
    private val placer = CallPlacer(nodeStore)

    fun allocateCall(callId: String, region: String): Call = nodeStore.withLock {
        placer.place(Call(callId, region)).getCall(callId)!!
    }


    fun getCall(callId: String): Call = nodeStore.withLock {
        nodeStore.findNodeWithCall(callId)?.getCall(callId)
            ?: throw CallNotFoundException(callId)
    }

}
