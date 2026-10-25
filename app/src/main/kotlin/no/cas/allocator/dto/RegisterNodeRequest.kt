package no.cas.allocator.dto

import kotlinx.serialization.Serializable
import no.cas.allocator.exceptions.ValidationException

@Serializable
data class RegisterNodeRequest(
    val id: String,
    val region: String,
    val capacity: Int,
    val currentCalls: Int
) {
    init {
        if (id.isBlank()) throw ValidationException("The node id cannot be blank")
        if (region.isBlank()) throw ValidationException("The node region cannot be blank")
        if (capacity < 1) throw ValidationException("The node capacity should be greater than 0.")
        if (currentCalls < 0) throw ValidationException("The node currentCalls cannot be negative")
    }
}

