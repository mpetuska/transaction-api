package lt.petuska.transaction.api.model

import lt.petuska.transaction.api.domain.*
import lt.petuska.transaction.api.domain.enumeration.*
import lt.petuska.transaction.api.domain.enumeration.Currency
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.*
import java.util.Date

object Transactions : IntIdTable() {
  val senderAccountId = integer("senderAccountId").references(Accounts.id).index("senderIdIdx")
  val receiverAccountId = integer("receiverAccountId").references(Accounts.id).index("receiverIdIdx")
  val senderCurrency = enumerationByName("senderCurrency", 3, Currency::class)
  val receiverCurrency = enumerationByName("receiverCurrency", 3, Currency::class)
  val sentAmount = double("amount")
  val status = enumerationByName("status", 10, TransactionStatus::class).default(TransactionStatus.PENDING)
  val issueDate = date("issueDate")
  val completionDate = date("completionDate").nullable()
  val completionMessage = varchar("completionMessage", 255).nullable()
  val receivedAmount = double("receivedAmount").nullable()
}

data class Transaction(
  override val id: Int,
  val senderAccountId: Int,
  val receiverAccountId: Int,
  val senderCurrency: Currency,
  val receiverCurrency: Currency,
  val sentAmount: Double,
  val status: TransactionStatus,
  val issueDate: Date,
  val completionDate: Date?,
  val completionMessage: String?,
  val receivedAmount: Double?
) : IntIdObj

data class NewTransaction(
  val senderAccountId: Int,
  val receiverAccountId: Int,
  val amount: Double
)

fun ResultRow.toTransaction(): Transaction {
  return Transaction(
    id = this[Transactions.id].value,
    senderAccountId = this[Transactions.senderAccountId],
    receiverAccountId = this[Transactions.receiverAccountId],
    senderCurrency = this[Transactions.senderCurrency],
    sentAmount = this[Transactions.sentAmount],
    status = this[Transactions.status],
    issueDate = this[Transactions.issueDate].toDate(),
    completionDate = this[Transactions.completionDate]?.toDate(),
    completionMessage = this[Transactions.completionMessage],
    receiverCurrency = this[Transactions.receiverCurrency],
    receivedAmount = this[Transactions.receivedAmount]
  )
}