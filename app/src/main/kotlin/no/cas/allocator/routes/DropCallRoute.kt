package no.cas.allocator.routes

import no.cas.allocator.services.CallService
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Path

class DropCallRoute(private val callService: CallService) : HttpHandler {
    private val idLens = Path.of("id")

    override fun invoke(request: Request): Response {
        callService.dropCall(idLens(request))
        return Response(Status.NO_CONTENT)
    }
}
