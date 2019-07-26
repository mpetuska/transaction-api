package lt.petuska.transaction.api.routing.transactions.transaction

import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import kotlinx.coroutines.*
import lt.petuska.transaction.api.domain.error.*
import lt.petuska.transaction.api.model.*
import lt.petuska.transaction.api.service.*
import lt.petuska.transaction.api.testutil.*
import org.spekframework.spek2.*
import org.spekframework.spek2.style.gherkin.*
import kotlin.test.*

@KtorExperimentalAPI
object GetTransactionRouteFeature : Spek({
  Feature("Get transaction") {
    Scenario("No transaction exist") {
      val testEngine = buildTestEngine(true)
      lateinit var call: TestApplicationCall

      When("Retrieving a transaction") {
        call = testEngine.handleRequest(HttpMethod.Get, "/v1/transactions/1")
      }

      Then("Response status is 404") {
        assertEquals(HttpStatusCode.NotFound, call.response.status())
      }

      Then("Error message is returned") {
        assertTrue(call.getBodyObject<ErrorMessage>().message.isNotBlank())
      }
    }

    Scenario("A transaction exists") {
      val testEngine = buildTestEngine(true)
      lateinit var call: TestApplicationCall
      lateinit var testUser: User
      lateinit var testAccount: Account
      lateinit var testAccount2: Account
      lateinit var testTransaction: Transaction

      Given("A user exists") {
        testUser = testEngine.createTestUser()
      }

      Given("A user has an account") {
        testAccount = testEngine.createTestAccount(testUser.id)
        testAccount2 = testEngine.createTestAccount(testUser.id)
      }

      Given("An account has transactions") {
        val transactionService by testEngine.dependency<TransactionService>()
        runBlocking {
          testTransaction = transactionService.issueTransaction(testAccount, testAccount2, 420.69).let {
            transactionService.getOne(it)
          }
          transactionService.issueTransaction(testAccount2, testAccount, 420.69)
        }
      }

      When("Getting a transaction") {
        call = testEngine.handleRequest(HttpMethod.Get, "/v1/transactions/${testTransaction.id}")
      }

      Then("Response status is 200") {
        assertEquals(HttpStatusCode.OK, call.response.status())
      }

      Then("A correct transaction is returned") {
        assertEquals(testTransaction, call.getBodyObject())
      }
    }
  }
})