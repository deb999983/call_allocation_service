package no.cas.allocator.db

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.cas.allocator.domain.CallNode
import java.util.UUID

class NodeStoreTest : DescribeSpec({
    // NodeStore is a shared singleton, so each test uses its own node id.
    describe("NodeStore") {
        it("returns a saved node together with its calls") {
            val nodeId = UUID.randomUUID().toString()
            val node = CallNode(nodeId, "eu-north", 3).apply { addCall("call-1") }

            NodeStore.saveNode(node)
            val stored = NodeStore.getNode(nodeId, "eu-north")

            stored.shouldNotBeNull()
            stored.capacity shouldBe 3
            stored.runningCallCount shouldBe 1
            stored.getCall("call-1")?.nodeId shouldBe nodeId
        }

        it("returns null for an unknown node") {
            NodeStore.getNode(UUID.randomUUID().toString(), "eu-north").shouldBeNull()
        }

        it("keeps nodes with the same id in different regions apart") {
            val nodeId = UUID.randomUUID().toString()
            NodeStore.saveNode(CallNode(nodeId, "eu-north", 3))

            NodeStore.getNode(nodeId, "us-east").shouldBeNull()
        }
    }
})
