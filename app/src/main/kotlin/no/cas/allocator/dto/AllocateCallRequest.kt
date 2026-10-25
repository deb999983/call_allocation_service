package no.cas.allocator.dto

data class AllocateCallRequest(
    val callId: String,
    val region: String
)