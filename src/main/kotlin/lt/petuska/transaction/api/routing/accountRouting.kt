package lt.petuska.transaction.api.routing

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import lt.petuska.transaction.api.domain.*
import lt.petuska.transaction.api.domain.cex.*
import lt.petuska.transaction.api.exception.*
import lt.petuska.transaction.api.model.*
import lt.petuska.transaction.api.model.Accounts.id
import lt.petuska.transaction.api.service.*
import org.kodein.di.generic.*
import org.kodein.di.ktor.*

fun Route.accounts() {
  route("accounts") {
    val accountService by kodein().instance<AccountService>()
    get {
      val accounts = accountService.getAll()
      call.respond(accounts)
    }
    route("{accountId}") {
      get {
        val accountId = getAccountId(false)
        val account = accountService.findOne {
          id eq accountId
        } ?: throw ApiException.NotFoundException("Account[id: $accountId] not found")
        call.respond(account)
      }
      delete {
        val accountId = getAccountId()
        val success = accountService.deleteOne {
          id eq accountId
        }
        call.respond(if (success) HttpStatusCode.NoContent else HttpStatusCode.InternalServerError)
      }
      put {
        val accountId = getAccountId()
        val newAccount = call.receive<NewAccount>()
        val success = accountService.update({ id eq accountId }) {
          it[name] = newAccount.name
          it[active] = newAccount.active
        }
        call.respond(if (success) HttpStatusCode.NoContent else HttpStatusCode.InternalServerError)
      }
      put("disable") {
        val accountId = getAccountId()
        val success = accountService.disableAccount(accountId)
        call.respond(if (success) HttpStatusCode.NoContent else HttpStatusCode.InternalServerError)
      }
      put("activate") {
        val accountId = getAccountId()
        val success = accountService.enableAccount(accountId)
        call.respond(if (success) HttpStatusCode.NoContent else HttpStatusCode.InternalServerError)
      }
      get("balance") {
        val accountId = getAccountId(false)
        val account = accountService.findOne { id eq accountId }
          ?: throw ApiException.NotFoundException("Account[id: $it] not found")
        val balance = accountService.getAccountBalance(accountId)
        call.respond(FieldResponse("balance", CexCurrency(balance, account.currency)))
      }
    }
  }
}

suspend fun PipelineContext<Unit, ApplicationCall>.getAccountId(checkExistence: Boolean = true): Int {
  return call.parameters["accountId"]?.toIntOrNull()?.also {
    if (checkExistence) {
      val accountService by kodein().instance<AccountService>()
      accountService.findOne { id eq it } ?: throw ApiException.NotFoundException("Account[id: $it] not found")
    }
  } ?: throw ApiException.BadRequestException("Invalid / missing account ID")
}