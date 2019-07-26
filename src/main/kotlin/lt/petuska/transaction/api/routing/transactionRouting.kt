package lt.petuska.transaction.api.routing

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import lt.petuska.transaction.api.domain.*
import lt.petuska.transaction.api.domain.enumeration.*
import lt.petuska.transaction.api.exception.*
import lt.petuska.transaction.api.model.*
import lt.petuska.transaction.api.service.*
import lt.petuska.transaction.api.util.*
import org.jetbrains.exposed.sql.*
import org.joda.time.*
import org.kodein.di.generic.*
import org.kodein.di.ktor.*

fun Route.transfers() {
  val transactionService by kodein().instance<TransactionService>()
  val userService by kodein().instance<UserService>()
  val accountService by kodein().instance<AccountService>()

  route("transactions") {
    post {
      val newTransaction = call.receive<NewTransaction>()
      val senderAccount = accountService.findOne { Accounts.id eq newTransaction.senderAccountId }
        ?: throw ApiException.NotFoundException("Sender Account[id: ${newTransaction.senderAccountId}] not found")
      val receiverAccount = accountService.findOne { Accounts.id eq newTransaction.receiverAccountId }
        ?: throw ApiException.NotFoundException("Receiver Account[id: ${newTransaction.receiverAccountId}] not found")

      val newTransactionId = transactionService.issueTransaction(senderAccount, receiverAccount, newTransaction.amount)
      call.respond(HttpStatusCode.Accepted, FieldResponse("id", newTransactionId))
    }
    get {
      val senderUserId = call.request.queryParameters["senderUserId"]?.toIntOrNull()
      val senderAccountId = call.request.queryParameters["senderAccountId"]?.toIntOrNull()
      val receiverUserId = call.request.queryParameters["receiverUserId"]?.toIntOrNull()
      val receiverAccountId = call.request.queryParameters["receiverAccountId"]?.toIntOrNull()
      val status = call.request.queryParameters["status"]?.let { TransactionStatus.valueOf(it) }
      val issuedAfter = call.request.queryParameters["issuedAfter"]?.let { DateTime.parse(it) }
      val issuedBefore = call.request.queryParameters["issuedBefore"]?.let { DateTime.parse(it) }
      val completedAfter = call.request.queryParameters["completedAfter"]?.let { DateTime.parse(it) }
      val completedBefore = call.request.queryParameters["completedBefore"]?.let { DateTime.parse(it) }

      if (hasNonNull(senderAccountId, senderUserId, receiverAccountId, receiverUserId)) {
        val senderAccountIds = senderUserId?.let { userId ->
          userService.getAllAccounts(userId).map { it.id }
        }
        val receiverAccountIds = receiverUserId?.let { userId ->
          userService.getAllAccounts(userId).map { it.id }
        }
        val filteredTransactions = transactionService.findAll {
          val conditions = mutableListOf<Op<Boolean>>().apply {
            senderAccountIds?.let { add(Transactions.senderAccountId inList it) }
            receiverAccountIds?.let { add(Transactions.receiverAccountId inList it) }
            senderAccountId?.let { add(Transactions.senderAccountId eq it) }
            receiverAccountId?.let { add(Transactions.receiverAccountId eq it) }
            status?.let { add(Transactions.status eq it) }
            issuedAfter?.let { add(Transactions.issueDate greater it) }
            completedAfter?.let { add(Transactions.completionDate greater it) }
            issuedBefore?.let { add(Transactions.issueDate less it) }
            completedBefore?.let { add(Transactions.completionDate less it) }
          }
          conditions.joinWithAnd()
        }

        call.respond(filteredTransactions)
      } else {
        throw ApiException.BadRequestException(
          "At least one of " +
              "[senderUserId, senderAccountId, receiverUserId, receiverAccountId] " +
              "query parameters is required"
        )
      }
    }
    route("{transactionId}") {
      get {
        val transactionId = getTransactionId(false)
        val transaction = transactionService.findOne {
          Transactions.id eq transactionId
        } ?: throw ApiException.NotFoundException("Transaction[id: $transactionId] not found")
        call.respond(transaction)
      }
      delete {
        val transactionId = getTransactionId()
        when (transactionService.getStatus(transactionId)) {
          TransactionStatus.SUCCESSFUL,
          TransactionStatus.TERMINATED -> throw ApiException.GoneException("Transaction[id: $transactionId] is already processed")
          TransactionStatus.PENDING -> {
            val success = transactionService.deleteOne {
              Transactions.id eq transactionId
            }
            call.respond(if (success) HttpStatusCode.NoContent else HttpStatusCode.InternalServerError)
          }
        }
      }
    }
  }
}

suspend fun PipelineContext<Unit, ApplicationCall>.getTransactionId(checkExistence: Boolean = true): Int {
  return call.parameters["transactionId"]?.toIntOrNull()?.also {
    if (checkExistence) {
      val transactionService by kodein().instance<TransactionService>()
      transactionService.findOne { Transactions.id eq it }
        ?: throw ApiException.NotFoundException("Transaction[id: $it] not found")
    }
  } ?: throw ApiException.BadRequestException("Invalid / missing transaction ID")
}