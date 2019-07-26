package lt.petuska.transaction.api.util

import io.ktor.application.*
import io.ktor.util.*

@KtorExperimentalAPI
inline fun <reified T> Application.getAppProperty(path: String): T {
  val propStr = environment.config.property(path).getString()
  return when (T::class) {
    Int::class -> propStr.toInt() as T
    Short::class -> propStr.toShort() as T
    Long::class -> propStr.toLong() as T
    Float::class -> propStr.toFloat() as T
    Double::class -> propStr.toDouble() as T
    Boolean::class -> propStr.toBoolean() as T
    else -> propStr as T
  }
}

fun hasNonNull(vararg objs: Any?) = objs.any { it != null }