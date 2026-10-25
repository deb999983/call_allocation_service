package no.cas.allocator.routes

import no.cas.allocator.dto.GetCallResponse
import no.cas.allocator.services.CallService
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.KotlinxSerialization.auto
import org.http4k.lens.Path

class GetCallRoute(private val callService: CallService) : HttpHandler {
    private val idLens = Path.of("id")
    private val responseLens = Body.auto<GetCallResponse>().toLens()

    override fun invoke(request: Request): Response {
        val call = callService.getCall(idLens(request))
        return Response(Status.OK).with(
            responseLens of GetCallResponse(call.id, call.region, call.nodeId, call.status)
        )
    }
}
