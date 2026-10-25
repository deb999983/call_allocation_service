package no.cas.allocator.filters

import no.cas.allocator.dto.ErrorResponse
import no.cas.allocator.exceptions.AllNodesFullException
import no.cas.allocator.exceptions.CallAlreadyFinishedException
import no.cas.allocator.exceptions.CallIdConflictException
import no.cas.allocator.exceptions.CallNotAllocatedToAnyNode
import no.cas.allocator.exceptions.CallNotFoundException
import no.cas.allocator.exceptions.NodeDoesNotExistException
import no.cas.allocator.exceptions.NodeDoesNotExistInRegionException
import no.cas.allocator.exceptions.ValidationException
import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CONFLICT
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.core.with
import org.http4k.lens.LensFailure

// Turns client errors into JSON error responses; anything unmapped is rethrown and becomes a 500 in CatchAll.
val ExceptionMapper = Filter { next ->
    { request ->
        try {
            next(request)
        } catch (e: LensFailure) {
            errorResponse(BAD_REQUEST, "Invalid request: ${e.message}")
        } catch (e: Exception) {
            val status = statusFor(e) ?: throw e
            errorResponse(status, e.message.orEmpty())
        }
    }
}

private fun statusFor(e: Exception): Status? = when (e) {
    is ValidationException -> BAD_REQUEST
    is NodeDoesNotExistException -> NOT_FOUND
    is NodeDoesNotExistInRegionException -> NOT_FOUND
    is CallNotFoundException -> NOT_FOUND
    is CallNotAllocatedToAnyNode -> NOT_FOUND
    is CallAlreadyFinishedException -> CONFLICT
    is CallIdConflictException -> CONFLICT
    is AllNodesFullException -> SERVICE_UNAVAILABLE
    else -> null
}

private fun errorResponse(status: Status, message: String) =
    Response(status).with(ErrorResponse.lens of ErrorResponse(message))
