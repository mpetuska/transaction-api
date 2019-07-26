package lt.petuska.transaction.api.service

import kotlinx.coroutines.*
import lt.petuska.transaction.api.domain.enumeration.*
import lt.petuska.transaction.api.model.*
import lt.petuska.transaction.api.util.*
import org.jetbrains.exposed.sql.*
import org.joda.time.*

class AccountService(
  database: Database,
  private val transactionService: TransactionService,
  rootAccountName: String
) : CrudIntIdDatabaseService<Accounts, Account>(database, Accounts) {

  private val rootAccount by lazy {
    runBlocking {
      dbQuery(database) {
        Accounts.select {
          Accounts.name eq rootAccountName
        }
          .mapNotNull(::toObject)
          .singleOrNull()
      }
    } ?: throw IllegalStateException("Unable to retrieve root account")
  }

  override fun toObject(row: ResultRow): Account {
    return row.toAccount()
  }

  suspend fun disableAccount(accountId: Int): Boolean {
    return update({ Accounts.id eq accountId }) {
      it[active] = false
    }
  }

  suspend fun enableAccount(accountId: Int): Boolean {
    return update({ Accounts.id eq accountId }) {
      it[active] = true
    }
  }

  suspend fun getAccountBalance(accountId: Int): Double {
    val inbound = transactionService.getSuccessfulInboundTransactions(accountId)
    val outbound = transactionService.getSuccessfulOutboundTransactions(accountId)
    return TransactionService.getBalance(inbound, outbound)
  }

  suspend fun issueStartingBalance(amount: Double, accountId: Int) {
    val account = getOne(accountId)
    transactionService.create {
      it[senderAccountId] = rootAccount.id
      it[receiverAccountId] = account.id
      it[senderCurrency] = account.currency
      it[receiverCurrency] = account.currency
      it[sentAmount] = amount
      it[status] = TransactionStatus.SUCCESSFUL
      it[issueDate] = DateTime()
      it[completionDate] = DateTime()
      it[receivedAmount] = amount
    }
  }

  override fun exclusionCheck(obj: Account): Boolean {
    return obj.name == rootAccount.name
  }
}