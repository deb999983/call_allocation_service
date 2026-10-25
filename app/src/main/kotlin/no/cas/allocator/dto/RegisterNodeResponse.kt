package no.cas.allocator.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterNodeResponse(val id: String, val region: String, val capacity: Int, val current: Int)
