package lt.petuska.transaction.api.service

import kotlinx.coroutines.*
import lt.petuska.transaction.api.domain.enumeration.*
import lt.petuska.transaction.api.model.*
import lt.petuska.transaction.api.model.Transaction
import org.jetbrains.exposed.sql.*
import org.joda.time.*

class TransactionService(
  database: Database,
  private val currencyExchangeService: CurrencyExchangeService,
  private val accountCheckService: AccountCheckService
) : CrudIntIdDatabaseService<Transactions, Transaction>(database, Transactions) {

  override fun toObject(row: ResultRow): Transaction {
    return row.toTransaction()
  }

  suspend fun issueTransaction(from: Account, to: Account, amount: Double): Int {
    return create {
      it[senderAccountId] = from.id
      it[receiverAccountId] = to.id
      it[senderCurrency] = from.currency
      it[receiverCurrency] = to.currency
      it[sentAmount] = amount
      it[issueDate] = DateTime()
    }.also {
      GlobalScope.launch {
        // This delay is not something we'd want in production,
        // but is used here to "simulate" some external api validation calls
        delay(5000)

        val transaction = getOne(it)
        val error = verifyTransaction(transaction)
        error?.let {
          terminateTransaction(transaction, error)
        } ?: finaliseTransaction(transaction)
      }
    }
  }

  suspend fun getStatus(transactionId: Int): TransactionStatus {
    return getOne(transactionId).status
  }

  suspend fun verifyTransaction(transaction: Transaction): String? {
    return when {
      !accountCheckService.isActive(transaction.senderAccountId) -> "Sender account is disabled"
      !accountCheckService.isActive(transaction.receiverAccountId) -> "Receiver account is disabled"
      !transaction.senderHasSufficientFunds() -> "Insufficient funds"
      else -> null
    }
  }

  private suspend fun Transaction.senderHasSufficientFunds(): Boolean {
    val inbound = getSuccessfulInboundTransactions(senderAccountId)
    val outbound = getSuccessfulOutboundTransactions(senderAccountId)
    val rawBalance = getBalance(inbound, outbound)
    return rawBalance >= sentAmount
  }

  suspend fun finaliseTransaction(transaction: Transaction) {
    val convertedAmount = try {
      currencyExchangeService.convert(
        transaction.sentAmount,
        transaction.senderCurrency,
        transaction.receiverCurrency
      )
    } catch (e: Exception) {
      terminateTransaction(transaction, "Unable to convert currency")
      return
    }

    update({ Transactions.id eq transaction.id }) {
      it[receivedAmount] = convertedAmount
      it[completionDate] = DateTime()
      it[status] = TransactionStatus.SUCCESSFUL
    }
  }

  suspend fun terminateTransaction(transaction: Transaction, reason: String? = null): TransactionStatus {
    return when (transaction.status) {
      TransactionStatus.PENDING -> {
        update({ Transactions.id eq transaction.id }) {
          it[status] = TransactionStatus.TERMINATED
          it[completionDate] = DateTime()
          it[completionMessage] = reason
        }
        TransactionStatus.TERMINATED
      }
      else -> transaction.status
    }
  }


  suspend fun getSuccessfulInboundTransactions(accountId: Int): List<Transaction> {
    return findAll {
      (Transactions.receiverAccountId eq accountId) and
          (Transactions.status eq TransactionStatus.SUCCESSFUL)
    }
  }

  suspend fun getSuccessfulOutboundTransactions(accountId: Int): List<Transaction> {
    return findAll {
      (Transactions.senderAccountId eq accountId) and
          (Transactions.status eq TransactionStatus.SUCCESSFUL)
    }
  }

  companion object {
    fun getBalance(inbound: List<Transaction>, outbound: List<Transaction>): Double {
      return inbound.sumByDouble { it.sentAmount } - outbound.sumByDouble { it.receivedAmount ?: 0.0 }
    }
  }
}