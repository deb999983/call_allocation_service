package no.cas.allocator.routes

import no.cas.allocator.dto.GetNodeResponse
import no.cas.allocator.services.NodeService
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.KotlinxSerialization.auto
import org.http4k.lens.Path

class GetNodeRoute(private val nodeService: NodeService) : HttpHandler {
    private val regionLens = Path.of("region")
    private val idLens = Path.of("id")
    private val responseLens = Body.auto<GetNodeResponse>().toLens()

    override fun invoke(request: Request): Response {
        val node = nodeService.getNode(idLens(request), regionLens(request))
        return Response(Status.OK).with(
            responseLens of GetNodeResponse(node.id, node.region, node.capacity, node.runningCallCount)
        )
    }
}
