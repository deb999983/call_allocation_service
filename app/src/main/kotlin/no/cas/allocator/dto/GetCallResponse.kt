package no.cas.allocator.dto

import kotlinx.serialization.Serializable
import no.cas.allocator.domain.CallStatus

@Serializable
data class GetCallResponse(val id: String, val region: String, val nodeId: String?, val status: CallStatus)
