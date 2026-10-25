package no.cas.allocator.utils

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import no.cas.allocator.db.NodeStore
import no.cas.allocator.domain.Call
import no.cas.allocator.domain.CallNode
import no.cas.allocator.domain.CallStatus
import no.cas.allocator.exceptions.CallAlreadyFinishedException
import java.util.UUID

class CallPlacerTest : DescribeSpec({
    val placer = CallPlacer(NodeStore)

    // NodeStore is a shared singleton, so each test uses its own region and ids.
    fun uniqueId() = UUID.randomUUID().toString()

    describe("CallPlacer") {
        it("throws CallAlreadyFinishedException when allocating a finished call again") {
            val region = uniqueId()
            val nodeId = uniqueId()
            val callId = uniqueId()
            val finishedCall = Call(callId, region, nodeId, CallStatus.FINISHED)
            NodeStore.saveNode(CallNode.restore(nodeId, region, capacity = 1, calls = listOf(finishedCall)))

            val exception = shouldThrow<CallAlreadyFinishedException> {
                placer.place(Call(callId, region))
            }

            exception.message shouldBe "Call $callId has already finished"
        }
    }
})
