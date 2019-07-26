package lt.petuska.transaction.api.testutil

import com.google.gson.*
import com.typesafe.config.*
import com.zaxxer.hikari.*
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import lt.petuska.transaction.api.*
import lt.petuska.transaction.api.config.*
import org.kodein.di.*
import org.kodein.di.generic.*
import org.kodein.di.ktor.*
import org.spekframework.spek2.dsl.*
import java.util.concurrent.*


val GSON = Gson()

fun kodeinConfig(block: Kodein.Builder.() -> Unit) = block

@KtorExperimentalAPI
fun LifecycleAware.buildTestEngine(
  bootstrap: Boolean = false,
  kodeinConfig: Kodein.Builder.() -> Unit = { registerBindings() }
): TestApplicationEngine {
  val testKodein = Kodein.Module("test", true) {
    kodeinConfig()
  }
  return TestApplicationEngine(createTestEnvironment {
    config = HoconApplicationConfig(ConfigFactory.load("application-test.conf"))
  }).also {
    beforeGroup {
      it.start(wait = false)
      it.application.module(testKodein, bootstrap)
    }
    afterGroup {
      val dataSource by it.dependency<HikariDataSource>("main")
      dataSource.close()
      it.stop(10, 20, TimeUnit.SECONDS)
    }
  }
}

fun TestApplicationRequest.setBodyJson(value: Any) {
  setBody(GSON.toJson(value))
  addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
}

inline fun <reified T : Any> TestApplicationCall.getBodyObject(): T {
  return GSON.fromJson(response.content, T::class.java)
}

fun <T : Any> T.toJsonAndBack(): T {
  return GSON.toJson(this).let { GSON.fromJson(it, this::class.java) }
}

inline fun <reified T : Any> TestApplicationCall.getBodyList(): List<T> {
  return mutableListOf<T>().apply {
    GSON.fromJson(response.content, JsonArray::class.java).forEach {
      add(GSON.fromJson(it.asJsonObject.toString(), T::class.java))
    }
  }
}

inline fun <reified T : Any> TestApplicationEngine.dependency(tag: String? = null): KodeinProperty<T> {
  return application.kodein().run {
    tag?.let {
      instance<T>(tag)
    } ?: instance<T>()
  }
}

inline fun <reified T : Any> TestApplicationEngine.lazyDependency(tag: String? = null) = lazy {
  val dep by dependency<T>(tag)
  dep
}

fun Any.toInt(): Int = (this as Number).toInt()

fun Map<String, Any>.toQueryString() = this.toList().joinToString("&") { "${it.first}=${it.second}" }

fun queryStringOf(builder: MutableMap<String, Any>.() -> Unit): String {
  return mutableMapOf<String, Any>().apply { builder() }.toQueryString()
}