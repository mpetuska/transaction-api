package lt.petuska.transaction.api.routing.transactions

import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import kotlinx.coroutines.*
import lt.petuska.transaction.api.domain.enumeration.*
import lt.petuska.transaction.api.domain.error.*
import lt.petuska.transaction.api.model.*
import lt.petuska.transaction.api.service.*
import lt.petuska.transaction.api.testutil.*
import org.spekframework.spek2.*
import org.spekframework.spek2.style.gherkin.*
import kotlin.test.*

@KtorExperimentalAPI
object FilterTransactionsRouteFeature : Spek({
  Feature("Filter transactions") {
    Scenario("No parameters are being sent") {
      val testEngine = buildTestEngine(true)
      lateinit var call: TestApplicationCall

      When("Filtering transactions") {
        call = testEngine.handleRequest(HttpMethod.Get, "/v1/transactions")
      }

      Then("Response status is 400") {
        assertEquals(HttpStatusCode.BadRequest, call.response.status())
      }

      Then("Error message is returned") {
        assertTrue(call.getBodyObject<ErrorMessage>().message.isNotBlank())
      }
    }

    Scenario("No ID parameters are being sent") {
      val testEngine = buildTestEngine(true)
      lateinit var call: TestApplicationCall

      When("Filtering transactions") {
        call = testEngine.handleRequest(HttpMethod.Get, "/v1/transactions?${queryStringOf {
          this["status"] = TransactionStatus.SUCCESSFUL
        }}")
      }

      Then("Response status is 400") {
        assertEquals(HttpStatusCode.BadRequest, call.response.status())
      }

      Then("Error message is returned") {
        assertTrue(call.getBodyObject<ErrorMessage>().message.isNotBlank())
      }
    }

    Scenario("Filter by senderUserId") {
      val testEngine = buildTestEngine(true)
      lateinit var call: TestApplicationCall
      lateinit var users: List<User>
      lateinit var accounts: List<Account>
      lateinit var otherTransactionIds: List<Int>
      lateinit var filteredTransactionIds: List<Int>

      Given("Multiple users exists") {
        users = testEngine.createTestUsers(3)
      }

      Given("They all have 2 accounts") {
        accounts = testEngine.createTestUsersAccounts(users, 2)
      }

      Given("There are outgoing transactions for a chosen user") {
        val transactionService by testEngine.dependency<TransactionService>()
        val otherTransIds = mutableListOf<Int>()
        val filteredTransIds = mutableListOf<Int>()

        runBlocking {
          accounts.filter { it.userId == users[0].id }.forEach { acc ->
            transactionService.issueTransaction(acc, accounts.find { it.userId != users[0].id }!!, 420.69)
              .let { filteredTransIds.add(it) }
          }
          transactionService.issueTransaction(
            accounts.find { it.userId != users[0].id }!!,
            accounts.find { it.userId == users[0].id }!!,
            420.69
          ).let { otherTransIds.add(it) }
        }
        filteredTransactionIds = filteredTransIds
        otherTransactionIds = otherTransIds
      }

      When("Filtering transactions") {
        call = testEngine.handleRequest(HttpMethod.Get, "/v1/transactions?${queryStringOf {
          this["senderAccountId"] = users[0].id
        }}")
      }

      Then("Response status is 200") {
        assertEquals(HttpStatusCode.OK, call.response.status())
      }

      Then("All expected transactions are returned") {
        println(call.getBodyList<Transaction>())
        assertTrue(call.getBodyList<Transaction>().all { filteredTransactionIds.contains(it.id) })
      }

      Then("None of unrelated transactions are returned") {
        assertTrue(call.getBodyList<Transaction>().none { otherTransactionIds.contains(it.id) })
      }
    }

    Scenario("Filter by receiverUserId") {
      val testEngine = buildTestEngine(true)
      lateinit var call: TestApplicationCall
      lateinit var users: List<User>
      lateinit var accounts: List<Account>
      lateinit var otherTransactionIds: List<Int>
      lateinit var filteredTransactionIds: List<Int>

      Given("Multiple users exists") {
        users = testEngine.createTestUsers(3)
      }

      Given("They all have 2 accounts") {
        accounts = testEngine.createTestUsersAccounts(users, 2)
      }

      Given("There are incoming transactions for a chosen user") {
        val transactionService by testEngine.dependency<TransactionService>()
        val otherTransIds = mutableListOf<Int>()
        val filteredTransIds = mutableListOf<Int>()

        runBlocking {
          accounts.filter { it.userId != users[0].id }.forEach { acc ->
            transactionService.issueTransaction(acc, accounts.find { it.userId == users[0].id }!!, 420.69)
              .let { filteredTransIds.add(it) }
          }
          transactionService.issueTransaction(
            accounts.find { it.userId == users[0].id }!!,
            accounts.find { it.userId != users[0].id }!!,
            420.69
          ).let { otherTransIds.add(it) }
        }
        filteredTransactionIds = filteredTransIds
        otherTransactionIds = otherTransIds
      }

      When("Filtering transactions") {
        call = testEngine.handleRequest(HttpMethod.Get, "/v1/transactions?${queryStringOf {
          this["receiverAccountId"] = users[0].id
        }}")
      }

      Then("Response status is 200") {
        assertEquals(HttpStatusCode.OK, call.response.status())
      }

      Then("All expected transactions are returned") {
        println(call.getBodyList<Transaction>())
        assertTrue(call.getBodyList<Transaction>().all { filteredTransactionIds.contains(it.id) })
      }

      Then("None of unrelated transactions are returned") {
        assertTrue(call.getBodyList<Transaction>().none { otherTransactionIds.contains(it.id) })
      }
    }

    Scenario("Filter by senderAccountId") {
      val testEngine = buildTestEngine(true)
      lateinit var call: TestApplicationCall
      lateinit var users: List<User>
      lateinit var accounts: List<Account>
      lateinit var otherTransactionIds: List<Int>
      lateinit var filteredTransactionIds: List<Int>

      Given("Multiple users exists") {
        users = testEngine.createTestUsers(3)
      }

      Given("They all have 2 accounts") {
        accounts = testEngine.createTestUsersAccounts(users, 2)
      }

      Given("There are outgoing transactions for a chosen account") {
        val transactionService by testEngine.dependency<TransactionService>()
        val otherTransIds = mutableListOf<Int>()
        val filteredTransIds = mutableListOf<Int>()
        runBlocking {
          repeat(accounts.size - 1) { i ->
            transactionService.issueTransaction(accounts[0], accounts[i + 1], 69.69).let { filteredTransIds.add(it) }
          }
          transactionService.issueTransaction(accounts[1], accounts[0], 420.20).let { otherTransIds.add(it) }
        }
        filteredTransactionIds = filteredTransIds
        otherTransactionIds = otherTransIds
      }

      When("Filtering transactions") {
        call = testEngine.handleRequest(HttpMethod.Get, "/v1/transactions?${queryStringOf {
          this["senderAccountId"] = accounts[0].id
        }}")
      }

      Then("Response status is 200") {
        assertEquals(HttpStatusCode.OK, call.response.status())
      }

      Then("All expected transactions are returned") {
        println(call.getBodyList<Transaction>())
        assertTrue(call.getBodyList<Transaction>().all { filteredTransactionIds.contains(it.id) })
      }

      Then("None of unrelated transactions are returned") {
        assertTrue(call.getBodyList<Transaction>().none { otherTransactionIds.contains(it.id) })
      }
    }

    Scenario("Filter by receiverAccountId") {
      val testEngine = buildTestEngine(true)
      lateinit var call: TestApplicationCall
      lateinit var users: List<User>
      lateinit var accounts: List<Account>
      lateinit var otherTransactionIds: List<Int>
      lateinit var filteredTransactionIds: List<Int>

      Given("Multiple users exists") {
        users = testEngine.createTestUsers(3)
      }

      Given("They all have 2 accounts") {
        accounts = testEngine.createTestUsersAccounts(users, 2)
      }

      Given("There are incoming transactions for a chosen account") {
        val transactionService by testEngine.dependency<TransactionService>()
        val otherTransIds = mutableListOf<Int>()
        val filteredTransIds = mutableListOf<Int>()
        runBlocking {
          repeat(accounts.size - 1) { i ->
            transactionService.issueTransaction(accounts[i + 1], accounts[0], 69.69).let { filteredTransIds.add(it) }
          }
          transactionService.issueTransaction(accounts[0], accounts[1], 420.20).let { otherTransIds.add(it) }
        }
        filteredTransactionIds = filteredTransIds
        otherTransactionIds = otherTransIds
      }

      When("Filtering transactions") {
        call = testEngine.handleRequest(HttpMethod.Get, "/v1/transactions?${queryStringOf {
          this["receiverAccountId"] = accounts[0].id
        }}")
      }

      Then("Response status is 200") {
        assertEquals(HttpStatusCode.OK, call.response.status())
      }

      Then("All expected transactions are returned") {
        println(call.getBodyList<Transaction>())
        assertTrue(call.getBodyList<Transaction>().all { filteredTransactionIds.contains(it.id) })
      }

      Then("None of unrelated transactions are returned") {
        assertTrue(call.getBodyList<Transaction>().none { otherTransactionIds.contains(it.id) })
      }
    }

    Scenario("Combo filter") {
      val testEngine = buildTestEngine(true)
      lateinit var call: TestApplicationCall
      lateinit var users: List<User>
      lateinit var accounts: List<Account>
      lateinit var otherTransactionIds: List<Int>
      lateinit var filteredTransactionIds: List<Int>

      Given("Multiple users exists") {
        users = testEngine.createTestUsers(3)
      }

      Given("They all have 2 accounts") {
        accounts = testEngine.createTestUsersAccounts(users, 2)
      }

      Given("There are incoming transactions for a chosen account from a chosen user") {
        val transactionService by testEngine.dependency<TransactionService>()
        val otherTransIds = mutableListOf<Int>()
        val filteredTransIds = mutableListOf<Int>()
        runBlocking {
          accounts.filter { it.userId == users[1].id }.forEach { acc ->
            transactionService.issueTransaction(acc, accounts[0], 420.69).let { filteredTransIds.add(it) }
          }
          transactionService.issueTransaction(accounts[0], accounts.find { it.userId != users[1].id }!!, 420.20)
            .let { otherTransIds.add(it) }
        }
        filteredTransactionIds = filteredTransIds
        otherTransactionIds = otherTransIds
      }

      When("Filtering transactions") {
        call = testEngine.handleRequest(HttpMethod.Get, "/v1/transactions?${queryStringOf {
          this["receiverAccountId"] = accounts[0].id
          this["senderUserId"] = users[1].id
        }}")
      }

      Then("Response status is 200") {
        assertEquals(HttpStatusCode.OK, call.response.status())
      }

      Then("All expected transactions are returned") {
        println(call.getBodyList<Transaction>())
        assertTrue(call.getBodyList<Transaction>().all { filteredTransactionIds.contains(it.id) })
      }

      Then("None of unrelated transactions are returned") {
        assertTrue(call.getBodyList<Transaction>().none { otherTransactionIds.contains(it.id) })
      }
    }
  }
})