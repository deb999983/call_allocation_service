package no.cas.allocator.dto

import kotlinx.serialization.Serializable

@Serializable
data class AllocateCallResponse(val nodeId: String, val region: String)
