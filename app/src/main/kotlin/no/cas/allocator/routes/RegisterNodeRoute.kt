package no.cas.allocator.routes

import no.cas.allocator.db.NodeStore
import no.cas.allocator.domain.CallNode
import no.cas.allocator.dto.RegisterNodeRequest
import no.cas.allocator.dto.RegisterNodeResponse
import no.cas.allocator.services.NodeService
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.KotlinxSerialization.auto

class RegisterNodeRoute(val nodeService: NodeService) : HttpHandler {
    private val requestLens = Body.auto<RegisterNodeRequest>().toLens()
    private val responseLens = Body.auto<RegisterNodeResponse>().toLens()

    override fun invoke(request: Request): Response {
        val request = requestLens(request)

        val node = nodeService.registerNode(request.id, request.region, request.capacity, request.currentCalls)
        return Response(Status.OK).with(
            responseLens of RegisterNodeResponse(node.id, node.region, node.capacity, node.currentCalls)
        )
    }
}
