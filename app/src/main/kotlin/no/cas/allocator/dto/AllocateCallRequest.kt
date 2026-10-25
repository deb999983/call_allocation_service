package no.cas.allocator.dto

import kotlinx.serialization.Serializable
import no.cas.allocator.exceptions.ValidationException

@Serializable
data class AllocateCallRequest(
    val callId: String,
    val region: String
) {
    init {
        if (callId.isBlank()) throw ValidationException("callId must not be blank")
        if (region.isBlank()) throw ValidationException("region must not be blank")
    }
}