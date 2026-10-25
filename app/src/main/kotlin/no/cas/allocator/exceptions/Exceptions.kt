package no.cas.allocator.exceptions

import no.cas.allocator.domain.Call
import no.cas.allocator.domain.CallStatus

class NodeDoesNotExistException(id: String, region: String) : RuntimeException("Node $id does not exist in $region")
class ValidationException(message: String) : RuntimeException(message)
