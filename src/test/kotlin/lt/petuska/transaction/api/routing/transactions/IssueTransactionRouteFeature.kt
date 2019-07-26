package lt.petuska.transaction.api.routing.transactions

import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import kotlinx.coroutines.*
import lt.petuska.transaction.api.domain.*
import lt.petuska.transaction.api.domain.enumeration.*
import lt.petuska.transaction.api.domain.error.*
import lt.petuska.transaction.api.model.*
import lt.petuska.transaction.api.service.*
import lt.petuska.transaction.api.testutil.*
import org.spekframework.spek2.*
import org.spekframework.spek2.style.gherkin.*
import kotlin.test.*

@KtorExperimentalAPI
object IssueTransactionRouteFeature : Spek({
  Feature("Issue transaction") {
    Scenario("Involved accounts do not exist") {
      val testEngine = buildTestEngine(true)
      lateinit var call: TestApplicationCall

      When("Issuing a transaction") {
        call = testEngine.handleRequest(HttpMethod.Post, "/v1/transactions") {
          setBodyJson(NewTransaction(6, 9, 69.69))
        }
      }

      Then("Response status is 404") {
        assertEquals(HttpStatusCode.NotFound, call.response.status())
      }

      Then("Error message is returned") {
        assertTrue(call.getBodyObject<ErrorMessage>().message.isNotBlank())
      }

      Then("An transaction is not issued") {
        val transactionService by testEngine.dependency<TransactionService>()
        runBlocking {
          assertTrue(transactionService.getAll().isEmpty())
        }
      }
    }

    Scenario("Involved accounts exist, but sender has insufficient funds") {
      val testEngine = buildTestEngine(true)
      lateinit var call: TestApplicationCall
      lateinit var testUser: User
      lateinit var senderAccount: Account
      lateinit var receiverAccount: Account
      val transactionId by lazy {
        call.getBodyObject<FieldResponse>().value.toInt()
      }

      Given("A user exists") {
        testUser = testEngine.createTestUser()
      }

      And("A user has two accounts with no cash") {
        senderAccount = testEngine.createTestAccount(testUser.id)
        receiverAccount = testEngine.createTestAccount(testUser.id)
      }

      When("Issuing a transaction") {
        call = testEngine.handleRequest(HttpMethod.Post, "/v1/transactions") {
          setBodyJson(NewTransaction(senderAccount.id, receiverAccount.id, 69.69))
        }
      }

      Then("Response status is 202") {
        assertEquals(HttpStatusCode.Accepted, call.response.status())
      }

      Then("An entity is returned") {
        assertNotNull(transactionId)
      }

      Then("A transaction is PENDING and takes 5s to process") {
        val transactionService by testEngine.dependency<TransactionService>()
        runBlocking {
          assertEquals(TransactionStatus.PENDING, transactionService.getOne(transactionId).status)
          delay(5500)
        }
      }

      Then("An transaction is terminated") {
        val transactionService by testEngine.dependency<TransactionService>()
        runBlocking {
          assertEquals(TransactionStatus.TERMINATED, transactionService.getOne(transactionId).status)
        }
      }
    }

    Scenario("Involved accounts exist and sender has sufficient funds") {
      val testEngine = buildTestEngine(true)
      lateinit var call: TestApplicationCall
      lateinit var testUser: User
      lateinit var senderAccount: Account
      lateinit var receiverAccount: Account
      val transactionId by lazy {
        call.getBodyObject<FieldResponse>().value.toInt()
      }

      Given("A user exists") {
        testUser = testEngine.createTestUser()
      }

      And("A user has two accounts with cash") {
        senderAccount = testEngine.createTestAccount(testUser.id, startingBalance = 1000.0)
        receiverAccount = testEngine.createTestAccount(testUser.id, startingBalance = 1000.0)
      }

      When("Issuing a transaction") {
        call = testEngine.handleRequest(HttpMethod.Post, "/v1/transactions") {
          setBodyJson(NewTransaction(senderAccount.id, receiverAccount.id, 500.0))
        }
      }

      Then("Response status is 202") {
        assertEquals(HttpStatusCode.Accepted, call.response.status())
      }

      Then("An entity is returned") {
        assertNotNull(transactionId)
      }

      Then("A transaction is PENDING and takes 5s to process") {
        val transactionService by testEngine.dependency<TransactionService>()
        runBlocking {
          assertEquals(TransactionStatus.PENDING, transactionService.getOne(transactionId).status)
          delay(5500)
        }
      }

      Then("An transaction is SUCCESSFUL") {
        val transactionService by testEngine.dependency<TransactionService>()
        runBlocking {
          assertEquals(TransactionStatus.SUCCESSFUL, transactionService.getOne(transactionId).status)
        }
      }

      Then("Money was transferred") {
        val accountService by testEngine.dependency<AccountService>()
        runBlocking {
          assertEquals(500.0, accountService.getAccountBalance(senderAccount.id))
          assertEquals(1500.0, accountService.getAccountBalance(receiverAccount.id))
        }
      }
    }
  }
})