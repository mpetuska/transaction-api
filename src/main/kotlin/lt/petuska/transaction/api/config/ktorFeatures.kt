package lt.petuska.transaction.api.config

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.util.*
import lt.petuska.transaction.api.exception.*
import org.slf4j.event.*

@KtorExperimentalAPI
fun Application.setupFeatures() {
  install(CallLogging) {
    level = Level.INFO
  }
  install(DefaultHeaders)
  install(ContentNegotiation) {
    gson {
    }
  }
  install(StatusPages) {
    exception<ContentTransformationException> { cause ->
      throw ApiException.BadRequestException(cause.message ?: "Invalid request body", cause.cause)
    }
    exception<ApiException> { cause ->
      call.respond(cause.code, cause.toErrorMessage())
    }
    exception<Exception> {
      call.respond(HttpStatusCode.InternalServerError)
    }
  }
}