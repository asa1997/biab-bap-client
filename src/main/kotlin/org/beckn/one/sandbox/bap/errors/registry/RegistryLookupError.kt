package org.beckn.one.sandbox.bap.errors.registry

import org.beckn.one.sandbox.bap.dtos.Error
import org.beckn.one.sandbox.bap.dtos.Response
import org.beckn.one.sandbox.bap.dtos.ResponseStatus
import org.beckn.one.sandbox.bap.errors.HttpError
import org.springframework.http.HttpStatus

sealed class RegistryLookupError : HttpError {
  val registryError = Error("BAP_001", "Registry lookup returned error")
  val nullResponseError = Error("BAP_002", "Registry lookup returned null")

  object RegistryError : RegistryLookupError() {
    override fun response(): Response =
      Response(status = ResponseStatus.NACK, message_id = null, error = registryError)

    override fun code(): HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR
  }

  object NullResponseError : RegistryLookupError() {
    override fun response(): Response =
      Response(status = ResponseStatus.NACK, message_id = null, error = nullResponseError)

    override fun code(): HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR
  }
}
