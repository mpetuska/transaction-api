package lt.petuska.transaction.api.service

import lt.petuska.transaction.api.domain.enumeration.*
import lt.petuska.transaction.api.exception.*
import lt.petuska.transaction.api.model.*
import lt.petuska.transaction.api.util.*
import org.jetbrains.exposed.sql.*

class AccountCheckService(
  private val database: Database,
  private val rootAccountName: String
) {

  suspend fun isActive(accountId: Int): Boolean {
    return getAccountById(accountId).active
  }

  suspend fun getAccountCurrency(accountId: Int): Currency {
    return getAccountById(accountId).currency
  }

  private suspend fun getAccountById(accountId: Int): Account {
    return dbQuery(database) {
      Accounts.select { Accounts.id eq accountId }
        .mapNotNull(ResultRow::toAccount)
        .single()
    }.takeIf { it.name != rootAccountName } ?: throw ApiException.BadRequestException("Account is locked")
  }
}