package no.cas.allocator.dto

import kotlinx.serialization.Serializable
import no.cas.allocator.exceptions.ValidationException

@Serializable
data class RegisterNodeRequest(
    val id: String,
    val region: String,
    val capacity: Int
) {
    init {
        if (id.isBlank()) throw ValidationException("The node id cannot be blank")
        if (region.isBlank()) throw ValidationException("The node region cannot be blank")
        if (capacity < 0) throw ValidationException("The node capacity must be positive.")
    }
}

