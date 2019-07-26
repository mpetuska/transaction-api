package lt.petuska.transaction.api.domain.error

import io.ktor.http.*

data class ErrorMessage(
  val code: Int,
  val message: String
) {
  constructor(code: HttpStatusCode, message: String) : this(code.value, message)
}