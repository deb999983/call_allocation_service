package no.cas.allocator.db

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.cas.allocator.domain.CallNode
import no.cas.allocator.exceptions.CallIdConflictException
import java.util.UUID

class NodeStoreTest : DescribeSpec({
    // NodeStore is a shared singleton, so each test uses its own node and call ids.
    describe("NodeStore") {
        it("returns a saved node together with its calls") {
            val nodeId = UUID.randomUUID().toString()
            val callId = UUID.randomUUID().toString()
            val node = CallNode(nodeId, "eu-north", 3).apply { addCall(callId) }

            NodeStore.saveNode(node)
            val stored = NodeStore.getNode(nodeId, "eu-north")

            stored.shouldNotBeNull()
            stored.capacity shouldBe 3
            stored.currentCalls shouldBe 1
            stored.getCall(callId)?.nodeId shouldBe nodeId
        }

        it("finds the node that holds a call") {
            val nodeId = UUID.randomUUID().toString()
            val callId = UUID.randomUUID().toString()
            NodeStore.saveNode(CallNode(nodeId, "eu-north", 3).apply { addCall(callId) })

            NodeStore.findNodeWithCall(callId)?.id shouldBe nodeId
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

    describe("NodeStore call id uniqueness") {
        fun uniqueId() = UUID.randomUUID().toString()

        it("rejects a call id that is already held by another node") {
            val callId = uniqueId()
            NodeStore.saveNode(CallNode(uniqueId(), "eu-north", 3).apply { addCall(callId) })
            val otherNodeId = uniqueId()

            shouldThrow<CallIdConflictException> {
                NodeStore.saveNode(CallNode(otherNodeId, "eu-north", 3).apply { addCall(callId) })
            }
            NodeStore.getNode(otherNodeId, "eu-north").shouldBeNull()
        }

        it("rejects a call id that is already held by a node in another region") {
            val callId = uniqueId()
            NodeStore.saveNode(CallNode(uniqueId(), "eu-north", 3).apply { addCall(callId) })

            shouldThrow<CallIdConflictException> {
                NodeStore.saveNode(CallNode(uniqueId(), "us-east", 3).apply { addCall(callId) })
            }
        }

        it("rejects a node holding the same call id twice") {
            val nodeId = uniqueId()
            val callId = uniqueId()

            shouldThrow<CallIdConflictException> {
                NodeStore.saveNode(CallNode(nodeId, "eu-north", 3).apply { addCall(callId); addCall(callId) })
            }
            NodeStore.getNode(nodeId, "eu-north").shouldBeNull()
        }

        it("keeps the original owner of a call id after a rejected save") {
            val nodeId = uniqueId()
            val callId = uniqueId()
            NodeStore.saveNode(CallNode(nodeId, "eu-north", 3).apply { addCall(callId) })

            shouldThrow<CallIdConflictException> {
                NodeStore.saveNode(CallNode(uniqueId(), "eu-north", 3).apply { addCall(callId) })
            }
            NodeStore.findNodeWithCall(callId)?.id shouldBe nodeId
        }

        it("releases a call id once the call is no longer on its node") {
            val nodeId = uniqueId()
            val callId = uniqueId()
            NodeStore.saveNode(CallNode(nodeId, "eu-north", 3).apply { addCall(callId) })

            NodeStore.saveNode(CallNode(nodeId, "eu-north", 3))
            NodeStore.findNodeWithCall(callId).shouldBeNull()

            val otherNodeId = uniqueId()
            NodeStore.saveNode(CallNode(otherNodeId, "eu-north", 3).apply { addCall(callId) })
            NodeStore.findNodeWithCall(callId)?.id shouldBe otherNodeId
        }

        it("allows saving the same node again with its existing calls") {
            val nodeId = uniqueId()
            val node = NodeStore.saveNode(CallNode(nodeId, "eu-north", 3).apply { addCall(uniqueId()) })

            NodeStore.saveNode(node.newReport(5, node.currentCalls))

            NodeStore.getNode(nodeId, "eu-north")?.capacity shouldBe 5
        }
    }
})
