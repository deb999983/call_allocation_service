package no.cas.allocator.routes

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import no.cas.allocator.app
import no.cas.allocator.domain.CallStatus
import no.cas.allocator.dto.AllocateCallRequest
import no.cas.allocator.dto.AllocateCallResponse
import no.cas.allocator.dto.GetCallResponse
import no.cas.allocator.dto.GetNodeResponse
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
    val getNodeResponseLens = Body.auto<GetNodeResponse>().toLens()

    // NodeStore is a shared singleton, so each test uses its own region and ids.
    fun uniqueId() = UUID.randomUUID().toString()

    fun register(nodeId: String, region: String, capacity: Int, currentCalls: Int = 0) =
        app(Request(Method.PUT, "/nodes").with(registerLens of RegisterNodeRequest(nodeId, region, capacity, currentCalls)))

    fun currentCalls(nodeId: String, region: String) =
        getNodeResponseLens(app(Request(Method.GET, "/nodes/$region/$nodeId"))).currentCalls

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

        it("returns 409 when the call id is already allocated in another region") {
            val region = uniqueId()
            val otherRegion = uniqueId()
            val callId = uniqueId()
            register(uniqueId(), region, capacity = 1)
            register(uniqueId(), otherRegion, capacity = 1)
            allocate(callId, region)

            allocate(callId, otherRegion).status shouldBe Status.CONFLICT
        }
    }

    describe("reported load") {
        it("does not allocate to a node that reports itself full") {
            val region = uniqueId()
            register(uniqueId(), region, capacity = 5, currentCalls = 5)

            allocate(uniqueId(), region).status shouldBe Status.SERVICE_UNAVAILABLE
        }

        it("prefers the node with the lower reported load") {
            val region = uniqueId()
            val busyNode = uniqueId()
            val idleNode = uniqueId()
            register(busyNode, region, capacity = 10, currentCalls = 8)
            register(idleNode, region, capacity = 10, currentCalls = 2)

            allocateResponseLens(allocate(uniqueId(), region)).nodeId shouldBe idleNode
        }

        it("adds calls placed since the last report to the reported load") {
            val region = uniqueId()
            val nodeId = uniqueId()
            register(nodeId, region, capacity = 100, currentCalls = 20)

            repeat(5) { allocate(uniqueId(), region) }

            currentCalls(nodeId, region) shouldBe 25
        }

        it("subtracts calls dropped since the last report") {
            val region = uniqueId()
            val nodeId = uniqueId()
            val callId = uniqueId()
            register(nodeId, region, capacity = 100, currentCalls = 20)
            allocate(callId, region)

            app(Request(Method.DELETE, "/calls/$callId"))

            currentCalls(nodeId, region) shouldBe 20
        }

        it("replaces the load with the next report, which already counts placed calls") {
            val region = uniqueId()
            val nodeId = uniqueId()
            register(nodeId, region, capacity = 100, currentCalls = 20)
            repeat(5) { allocate(uniqueId(), region) }

            register(nodeId, region, capacity = 100, currentCalls = 23)

            currentCalls(nodeId, region) shouldBe 23
        }

        it("does not undercount when a call the report already excluded is dropped") {
            val region = uniqueId()
            val nodeId = uniqueId()
            val callId = uniqueId()
            register(nodeId, region, capacity = 100)
            allocate(callId, region)
            // The call ended on the node before its DELETE reached us, so the report no longer counts it.
            register(nodeId, region, capacity = 100, currentCalls = 0)
            app(Request(Method.DELETE, "/calls/$callId"))

            allocate(uniqueId(), region)

            currentCalls(nodeId, region) shouldBe 1
        }

        it("keeps call affinity across reports") {
            val region = uniqueId()
            val nodeId = uniqueId()
            val callId = uniqueId()
            register(nodeId, region, capacity = 1)
            allocate(callId, region)

            register(nodeId, region, capacity = 1, currentCalls = 1)

            allocateResponseLens(allocate(callId, region)).nodeId shouldBe nodeId
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

    describe("DELETE /calls/{id}") {
        fun drop(callId: String) = app(Request(Method.DELETE, "/calls/$callId"))

        it("marks the call as finished") {
            val region = uniqueId()
            val nodeId = uniqueId()
            val callId = uniqueId()
            register(nodeId, region, capacity = 1)
            allocate(callId, region)

            drop(callId).status shouldBe Status.NO_CONTENT

            val response = app(Request(Method.GET, "/calls/$callId"))
            getCallResponseLens(response) shouldBe GetCallResponse(callId, region, nodeId, CallStatus.FINISHED)
        }

        it("frees capacity on the node for new calls") {
            val region = uniqueId()
            val callId = uniqueId()
            register(uniqueId(), region, capacity = 1)
            allocate(callId, region)

            drop(callId)

            allocate(uniqueId(), region).status shouldBe Status.OK
        }

        it("returns 409 when the call is dropped again") {
            val region = uniqueId()
            val callId = uniqueId()
            register(uniqueId(), region, capacity = 1)
            allocate(callId, region)
            drop(callId)

            drop(callId).status shouldBe Status.CONFLICT
        }

        it("returns 409 when a dropped call is allocated again") {
            val region = uniqueId()
            val callId = uniqueId()
            register(uniqueId(), region, capacity = 1)
            allocate(callId, region)
            drop(callId)

            allocate(callId, region).status shouldBe Status.CONFLICT
        }

        it("returns 404 for an unknown call") {
            drop(uniqueId()).status shouldBe Status.NOT_FOUND
        }
    }
})
