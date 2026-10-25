package no.cas.allocator.dto

import kotlinx.serialization.Serializable

@Serializable
data class GetNodeResponse(val id: String, val region: String, val capacity: Int, val currentCalls: Int)
