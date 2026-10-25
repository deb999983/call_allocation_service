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

    var reportedCalls: Int = 0
        private set
    var callsSinceReport: Int = 0
        private set

    val calls: List<Call> get() = allCalls

    val currentCalls: Int get() = reportedCalls + callsSinceReport

    fun loadFactor(): Double = currentCalls.toDouble() / capacity

    fun canAllocateNewCalls(): Boolean {
        return currentCalls < capacity
    }

    fun getCall(id: String): Call? {
        return allCalls.find { it.id == id }
    }

    fun addCall(callId: String) {
        val call = Call(callId, region, id, CallStatus.RUNNING)
        runningCalls.add(call)
        callsSinceReport++
    }

    fun newReport(newCapacity: Int, newCurrentCalls: Int): CallNode {
        capacity = newCapacity
        reportedCalls = newCurrentCalls
        callsSinceReport = 0
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

        if (currentCalls > 0) callsSinceReport--
    }

    fun dropCall(callId: String) {
        val call = getCall(callId)?:return
        dropCall(call)
    }


    companion object {
        fun restore(
            id: String,
            region: String,
            capacity: Int,
            calls: List<Call>,
            reportedCalls: Int = 0,
            callsSinceReport: Int = 0
        ): CallNode {
            val node = CallNode(id, region, capacity)
            calls.forEach {
                if (it.status == CallStatus.RUNNING) node.runningCalls.add(it) else node.stoppedCalls.add(it)
            }
            node.reportedCalls = reportedCalls
            node.callsSinceReport = callsSinceReport
            return node
        }
    }
}
