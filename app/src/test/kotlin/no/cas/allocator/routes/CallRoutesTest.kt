package no.cas.allocator.routes

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import no.cas.allocator.app
import no.cas.allocator.domain.CallStatus
import no.cas.allocator.dto.AllocateCallRequest
import no.cas.allocator.dto.AllocateCallResponse
import no.cas.allocator.dto.GetCallResponse
import no.cas.allocator.dto.RegisterNodeRequest
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.KotlinxSerialization.auto
import java.util.UUID

class CallRoutesTest : DescribeSpec({
    val app = app()
    val registerLens = Body.auto<RegisterNodeRequest>().toLens()
    val allocateLens = Body.auto<AllocateCallRequest>().toLens()
    val allocateResponseLens = Body.auto<AllocateCallResponse>().toLens()
    val getCallResponseLens = Body.auto<GetCallResponse>().toLens()

    // NodeStore is a shared singleton, so each test uses its own region and ids.
    fun uniqueId() = UUID.randomUUID().toString()

    fun register(nodeId: String, region: String, capacity: Int) =
        app(Request(Method.PUT, "/nodes").with(registerLens of RegisterNodeRequest(nodeId, region, capacity)))

    fun allocate(callId: String, region: String) =
        app(Request(Method.POST, "/calls").with(allocateLens of AllocateCallRequest(callId, region)))

    describe("POST /calls") {
        it("allocates a call to a node in the region") {
            val region = uniqueId()
            val nodeId = uniqueId()
            register(nodeId, region, capacity = 1)

            val response = allocate(uniqueId(), region)

            response.status shouldBe Status.OK
            allocateResponseLens(response) shouldBe AllocateCallResponse(nodeId, region)
        }

        it("returns the same node when the call is allocated again") {
            val region = uniqueId()
            register(uniqueId(), region, capacity = 1)
            register(uniqueId(), region, capacity = 1)
            val callId = uniqueId()
            val first = allocateResponseLens(allocate(callId, region))

            val second = allocate(callId, region)

            second.status shouldBe Status.OK
            allocateResponseLens(second) shouldBe first
        }

        it("returns 404 when the region has no nodes") {
            allocate(uniqueId(), uniqueId()).status shouldBe Status.NOT_FOUND
        }

        it("returns 503 when all nodes in the region are full") {
            val region = uniqueId()
            register(uniqueId(), region, capacity = 1)
            allocate(uniqueId(), region)

            allocate(uniqueId(), region).status shouldBe Status.SERVICE_UNAVAILABLE
        }

        it("returns 400 for a blank call id") {
            val response = app(
                Request(Method.POST, "/calls").body("""{"callId": " ", "region": "eu-north"}""")
            )

            response.status shouldBe Status.BAD_REQUEST
        }
    }

    describe("GET /calls/{id}") {
        it("returns the details of an allocated call") {
            val region = uniqueId()
            val nodeId = uniqueId()
            val callId = uniqueId()
            register(nodeId, region, capacity = 1)
            allocate(callId, region)

            val response = app(Request(Method.GET, "/calls/$callId"))

            response.status shouldBe Status.OK
            getCallResponseLens(response) shouldBe GetCallResponse(callId, region, nodeId, CallStatus.RUNNING)
        }

        it("returns 404 for an unknown call") {
            app(Request(Method.GET, "/calls/${uniqueId()}")).status shouldBe Status.NOT_FOUND
        }
    }
})
