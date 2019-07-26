package lt.petuska.transaction.api.service

import lt.petuska.transaction.api.model.*
import org.jetbrains.exposed.sql.*

class UserService(
  database: Database,
  private val accountService: AccountService,
  private val rootUserName: String
) : CrudIntIdDatabaseService<Users, User>(database, Users) {

  override fun toObject(row: ResultRow): User {
    return row.toUser()
  }

  override suspend fun deleteOne(selector: SqlExpressionBuilder.() -> Op<Boolean>): Boolean {
    findOne(selector)?.let {
      accountService.deleteAll { Accounts.userId eq it.id }
    }
    return super.deleteOne(selector)
  }

  override suspend fun deleteAll(selector: SqlExpressionBuilder.() -> Op<Boolean>): Int {
    findAll(selector).forEach {
      accountService.deleteAll { Accounts.userId eq it.id }
    }
    return super.deleteAll(selector)
  }

  suspend fun getAllAccounts(userId: Int): List<Account> {
    return accountService.findAll { Accounts.userId eq userId }
  }

  override fun exclusionCheck(obj: User): Boolean {
    return obj.name == rootUserName
  }
}