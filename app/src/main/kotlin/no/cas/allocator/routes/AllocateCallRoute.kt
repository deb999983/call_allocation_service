package no.cas.allocator.routes

import no.cas.allocator.dto.AllocateCallRequest
import no.cas.allocator.dto.AllocateCallResponse
import no.cas.allocator.services.CallService
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.KotlinxSerialization.auto

class AllocateCallRoute(private val callService: CallService) : HttpHandler {
    private val requestLens = Body.auto<AllocateCallRequest>().toLens()
    private val responseLens = Body.auto<AllocateCallResponse>().toLens()

    override fun invoke(request: Request): Response {
        val request = requestLens(request)

        val call = callService.allocateCall(request.callId, request.region)
        return Response(Status.OK).with(
            responseLens of AllocateCallResponse(call.nodeId!!, call.region)
        )
    }
}
