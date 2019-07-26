package lt.petuska.transaction.api.util

import kotlinx.coroutines.*
import lt.petuska.transaction.api.exception.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*

suspend fun <T> dbQuery(database: Database? = null, block: () -> T): T {
  return withContext(Dispatchers.IO) {
    try {
      database?.let {
        transaction(it) { block() }
      } ?: transaction { block() }
    } catch (e: Throwable) {
      throw ApiException.BadGatewayException("Error querying the database", e)
    }
  }
}

fun List<Op<Boolean>>.joinWithAnd(): Op<Boolean> {
  return when (size) {
    0 -> Op.TRUE
    1 -> this[0]
    else -> this[0] and this.subList(1, size).joinWithAnd()
  }
}
