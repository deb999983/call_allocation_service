package no.cas.allocator.domain

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