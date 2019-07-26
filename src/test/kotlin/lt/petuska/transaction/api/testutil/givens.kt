package lt.petuska.transaction.api.testutil

import io.ktor.server.testing.*
import kotlinx.coroutines.*
import lt.petuska.transaction.api.domain.enumeration.*
import lt.petuska.transaction.api.model.*
import lt.petuska.transaction.api.service.*
import org.kodein.di.generic.*
import org.kodein.di.ktor.*

fun TestApplicationEngine.createTestUser(name: String = "John Doe"): User {
  val userService by application.kodein().instance<UserService>()
  return runBlocking {
    userService.create {
      it[Users.name] = name
    }.let { userService.getOne(it) }
  }
}

fun TestApplicationEngine.createTestUsers(count: Int): List<User> {
  return mutableListOf<User>().apply {
    repeat(count) { i ->
      add(createTestUser("John Doe #$i"))
    }
  }
}

fun TestApplicationEngine.createTestUsersAccounts(users: List<User>, accountsPerUser: Int = 1): List<Account> {
  return mutableListOf<Account>().apply {
    users.forEach { u ->
      repeat(accountsPerUser) { i ->
        add(createTestAccount(u.id, "John Doe's Account #$i"))
      }
    }
  }
}

fun TestApplicationEngine.createTestAccount(
  userId: Int,
  name: String = "John Doe's Test Account",
  currency: Currency = Currency.EUR,
  active: Boolean = true,
  startingBalance: Double? = null
): Account {
  val accountService by application.kodein().instance<AccountService>()
  return runBlocking {
    accountService.create {
      it[Accounts.userId] = userId
      it[Accounts.name] = name
      it[Accounts.currency] = currency
      it[Accounts.active] = active
    }.let { accountService.getOne(it) }.also { acc ->
      startingBalance?.let { accountService.issueStartingBalance(startingBalance, acc.id) }
    }
  }
}