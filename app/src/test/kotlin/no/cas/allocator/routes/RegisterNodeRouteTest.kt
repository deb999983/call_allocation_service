package no.cas.allocator.routes

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import no.cas.allocator.app
import no.cas.allocator.db.NodeStore
import no.cas.allocator.dto.RegisterNodeRequest
import no.cas.allocator.dto.RegisterNodeResponse
import no.cas.allocator.services.NodeService
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.KotlinxSerialization.auto
import java.util.UUID

class RegisterNodeRouteTest : DescribeSpec({
    val route = RegisterNodeRoute(NodeService(NodeStore))
    val app = app()
    val requestLens = Body.auto<RegisterNodeRequest>().toLens()
    val responseLens = Body.auto<RegisterNodeResponse>().toLens()

    fun register(nodeId: String, capacity: Int, currentCalls: Int = 0) =
        route(Request(Method.PUT, "/nodes").with(requestLens of RegisterNodeRequest(nodeId, "eu-north", capacity, currentCalls)))

    describe("PUT /nodes") {
        // NodeStore is a shared singleton, so each test uses its own node id.
        it("registers a new node") {
            val nodeId = UUID.randomUUID().toString()

            val response = register(nodeId, capacity = 5)

            response.status shouldBe Status.OK
            responseLens(response) shouldBe RegisterNodeResponse(nodeId, "eu-north", 5, 0)
        }

        it("updates the capacity when an existing node re-registers") {
            val nodeId = UUID.randomUUID().toString()
            register(nodeId, capacity = 5)

            val response = register(nodeId, capacity = 10)

            response.status shouldBe Status.OK
            responseLens(response).capacity shouldBe 10
        }

        it("returns the reported current calls") {
            val nodeId = UUID.randomUUID().toString()

            val response = register(nodeId, capacity = 100, currentCalls = 20)

            responseLens(response) shouldBe RegisterNodeResponse(nodeId, "eu-north", 100, 20)
        }

        it("returns 400 for negative current calls") {
            val response = app(
                Request(Method.PUT, "/nodes")
                    .body("""{"id": "n", "region": "eu-north", "capacity": 5, "currentCalls": -1}""")
            )

            response.status shouldBe Status.BAD_REQUEST
        }

        it("returns 400 when current calls are missing") {
            val response = app(
                Request(Method.PUT, "/nodes").body("""{"id": "n", "region": "eu-north", "capacity": 5}""")
            )

            response.status shouldBe Status.BAD_REQUEST
        }
    }
})
