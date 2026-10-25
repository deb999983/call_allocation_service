package no.cas.allocator.domain

import no.cas.allocator.exceptions.CallHasInvalidStatusException
import no.cas.allocator.exceptions.CallNodeIdMismatchException

data class CallNode(
    val id: String,
    val region: String,
    var capacity: Int
) {
    private val runningCalls = mutableListOf<Call>()
    private val stoppedCalls = mutableListOf<Call>()
    private val allCalls : List<Call> get() = runningCalls + stoppedCalls

    val calls: List<Call> get() = allCalls

    val runningCallCount: Int get() = runningCalls.size

    fun loadFactor(): Double = runningCalls.size.toDouble() / capacity

    fun canAllocateNewCalls(): Boolean {
        return runningCalls.size < capacity
    }

    fun getCall(id: String): Call? {
        return allCalls.find { it.id == id }
    }

    fun addCall(callId: String) {
        val call = Call(callId, region, id, CallStatus.RUNNING)
        runningCalls.add(call)
    }

    fun updateCapacity(newCapacity: Int): CallNode {
        capacity = newCapacity
        return this
    }

    fun dropCall(call: Call) {
        if (call.nodeId != this.id) {
            throw CallNodeIdMismatchException(call, this.id)
        }

        if (call.status != CallStatus.RUNNING) {
            throw CallHasInvalidStatusException(call, call.status, CallStatus.RUNNING)
        }

        runningCalls.remove(call)
        stoppedCalls.add(call.copy(status = CallStatus.FINISHED))
    }

    fun dropCall(callId: String) {
        val call = getCall(callId)?:return
        dropCall(call)
    }


    companion object {
        fun restore(id: String, region: String, capacity: Int, calls: List<Call>): CallNode {
            val node = CallNode(id, region, capacity)
            calls.forEach {
                if (it.status == CallStatus.RUNNING) node.runningCalls.add(it) else node.stoppedCalls.add(it)
            }
            return node
        }
    }
}