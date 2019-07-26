package lt.petuska.transaction.api.config

import io.ktor.application.*
import kotlinx.coroutines.*
import lt.petuska.transaction.api.domain.enumeration.*
import lt.petuska.transaction.api.model.*
import lt.petuska.transaction.api.service.*
import org.kodein.di.generic.*
import org.kodein.di.ktor.*

fun Application.bootstrapDatabase() {
  val userService by kodein().instance<UserService>()
  val rootUserName by kodein().instance<String>("rootUserName")
  val accountService by kodein().instance<AccountService>()
  val rootAccountName by kodein().instance<String>("rootAccountName")
  val rootAccountCurrency by kodein().instance<String>("rootAccountCurrency")
  runBlocking {
    val rootUserId = userService.findOne { Users.name eq rootUserName }?.id
      ?: userService.create {
        it[name] = rootUserName
      }
    accountService.findOne { Accounts.name eq rootAccountName }?.id
      ?: accountService.create {
        it[userId] = rootUserId
        it[name] = rootAccountName
        it[currency] = Currency.valueOf(rootAccountCurrency)
      }
  }
}