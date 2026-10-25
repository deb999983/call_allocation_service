package no.cas.allocator.dto

import kotlinx.serialization.Serializable
import org.http4k.core.Body
import org.http4k.format.KotlinxSerialization.auto

@Serializable
data class ErrorResponse(val message: String){
    companion object {
        val lens = Body.auto<ErrorResponse>().toLens()
    }
}
