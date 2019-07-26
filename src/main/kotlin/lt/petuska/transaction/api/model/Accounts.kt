package lt.petuska.transaction.api.model

import lt.petuska.transaction.api.domain.*
import lt.petuska.transaction.api.domain.enumeration.*
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.*

object Accounts : IntIdTable() {
  val userId = integer("userId").references(Users.id)
  val name = varchar("name", 255)
  val currency = enumerationByName("currency", 3, Currency::class)
  val active = bool("active").default(true)
}

data class Account(
  override val id: Int,
  val userId: Int,
  val name: String,
  val currency: Currency,
  val active: Boolean
) : IntIdObj

data class NewAccount(
  val name: String,
  val currency: Currency,
  val active: Boolean = true,
  //Only here for demonstration purposes.
  val startingBalance: Double? = null
)

fun ResultRow.toAccount(): Account {
  return Account(
    id = this[Accounts.id].value,
    userId = this[Accounts.userId],
    name = this[Accounts.name],
    currency = this[Accounts.currency],
    active = this[Accounts.active]
  )
}