package lt.petuska.transaction.api.model

import lt.petuska.transaction.api.domain.*
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.*

object Users : IntIdTable() {
  val name = varchar("name", 255)
}

data class User(
  override val id: Int,
  val name: String
) : IntIdObj

data class NewUser(
  val name: String
)

fun ResultRow.toUser(): User {
  return User(
    id = this[Users.id].value,
    name = this[Users.name]
  )
}