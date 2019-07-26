package lt.petuska.transaction.api.routing

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import lt.petuska.transaction.api.domain.*
import lt.petuska.transaction.api.exception.*
import lt.petuska.transaction.api.model.*
import lt.petuska.transaction.api.model.Users.id
import lt.petuska.transaction.api.service.*
import org.kodein.di.generic.*
import org.kodein.di.ktor.*

fun Route.users() {
  route("users") {
    val userService by kodein().instance<UserService>()
    get {
      call.respond(userService.getAll())
    }
    post {
      val newUser = call.receive<NewUser>()
      val newUserId = userService.create {
        it[name] = newUser.name
      }
      call.respond(HttpStatusCode.Created, FieldResponse("id", newUserId))
    }
    route("{userId}") {
      get {
        val userId = getUserId(false)
        val user = userService.findOne {
          id eq userId
        } ?: throw ApiException.NotFoundException("User[id: $userId] not found")
        call.respond(user)
      }
      delete {
        val userId = getUserId()
        val success = userService.deleteOne {
          id eq userId
        }
        call.respond(if (success) HttpStatusCode.NoContent else HttpStatusCode.InternalServerError)
      }
      put {
        val userId = getUserId()
        val newUser = call.receive<NewUser>()
        val success = userService.update({ id eq userId }) {
          it[name] = newUser.name
        }
        call.respond(if (success) HttpStatusCode.NoContent else HttpStatusCode.InternalServerError)
      }
      route("accounts") {
        val accountService by kodein().instance<AccountService>()
        post {
          val usrId = getUserId()
          val newAccount = call.receive<NewAccount>()
          val newAccountId = accountService.create {
            it[userId] = usrId
            it[name] = newAccount.name
            it[currency] = newAccount.currency
            it[active] = newAccount.active
          }
          newAccount.startingBalance?.let {
            accountService.issueStartingBalance(it, newAccountId)
          }
          call.respond(HttpStatusCode.Created, FieldResponse("id", newAccountId))
        }
        get {
          val userId = getUserId()
          val accounts = accountService.findAll {
            Accounts.userId eq userId
          }
          call.respond(accounts)
        }
      }
    }
  }
}

suspend fun PipelineContext<Unit, ApplicationCall>.getUserId(checkExistence: Boolean = true): Int {
  return call.parameters["userId"]?.toIntOrNull()?.also {
    if (checkExistence) {
      val userService by kodein().instance<UserService>()
      userService.findOne { id eq it } ?: throw ApiException.NotFoundException("User[id: $it] not found")
    }
  } ?: throw ApiException.BadRequestException("Invalid / missing users ID")
}