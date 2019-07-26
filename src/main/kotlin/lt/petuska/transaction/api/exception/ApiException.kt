package lt.petuska.transaction.api.exception

import io.ktor.http.*
import lt.petuska.transaction.api.domain.error.*


sealed class ApiException(val code: HttpStatusCode, override val message: String, cause: Throwable? = null) :
  RuntimeException(message, cause) {
  class BadRequestException(message: String, cause: Throwable? = null) :
    ApiException(HttpStatusCode.BadRequest, message, cause)

  class NotFoundException(message: String, cause: Throwable? = null) :
    ApiException(HttpStatusCode.NotFound, message, cause)

  class BadGatewayException(message: String, cause: Throwable? = null) :
    ApiException(HttpStatusCode.BadGateway, message, cause)

  class GoneException(message: String, cause: Throwable? = null) :
    ApiException(HttpStatusCode.Gone, message, cause)

  fun toErrorMessage(): ErrorMessage {
    return ErrorMessage(code, message)
  }
}