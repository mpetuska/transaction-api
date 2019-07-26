package lt.petuska.transaction.api

import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.util.*
import lt.petuska.transaction.api.config.*
import lt.petuska.transaction.api.routing.*
import org.kodein.di.*
import org.kodein.di.ktor.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalAPI
fun Application.module(
  kodein: Kodein.Module = Kodein.Module("main") { registerBindings() },
  bootstrap: Boolean = true
) {
  kodein {
    import(kodein, allowOverride = true)
  }
  setupFeatures()
  if (bootstrap) {
    bootstrapDatabase()
  }
  routing {
    route("v1") {
      users()
      accounts()
      transfers()
    }
  }
}