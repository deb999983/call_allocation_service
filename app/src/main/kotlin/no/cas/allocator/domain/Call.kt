package no.cas.allocator.domain

enum class CallStatus {
    PENDING,
    RUNNING,
    FINISHED
}

data class Call(
    val id: String,
    val region: String,
    val nodeId: String? = null,
    val status: CallStatus = CallStatus.PENDING
)