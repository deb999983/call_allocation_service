package no.cas.allocator.exceptions

import no.cas.allocator.domain.Call
import no.cas.allocator.domain.CallStatus

class NodeDoesNotExistException(id: String, region: String) : RuntimeException("Node $id does not exist in $region")
class NodeDoesNotExistInRegionException(region: String) : RuntimeException("No node exists in $region")
class AllNodesFullException(callId: String, region: String) : RuntimeException("All nodes in $region are full, call $callId was not allocated")
class ValidationException(message: String) : RuntimeException(message)

class CallAlreadyFinishedException(callId: String) : RuntimeException("Call $callId has already finished")
class CallNotFoundException(callId: String) : RuntimeException("Call $callId not found")
class CallIdConflictException(callId: String) : RuntimeException("Call $callId is already allocated to another node")
class CallNodeIdMismatchException(call: Call, otherNodeId: String) : RuntimeException("Call $call does not match node $otherNodeId")
class CallHasInvalidStatusException(call: Call, status: CallStatus, expectedStatus: CallStatus) : RuntimeException("Call $call status is invalid: $status, expectedStatus: $expectedStatus")
class CallNotAllocatedToAnyNodeException(callId: String) : RuntimeException("Call $callId is not running or queued")
