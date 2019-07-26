package lt.petuska.transaction.api.routing.accounts.account

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
object DeleteAccountRouteFeature : Spek({
  Feature("Delete account") {
    Scenario("No account exist") {
      val testEngine = buildTestEngine()
      lateinit var call: TestApplicationCall
      When("Deleting an account") {
        call = testEngine.handleRequest(HttpMethod.Delete, "/v1/accounts/1")
      }

      Then("Response status is 404") {
        assertEquals(HttpStatusCode.NotFound, call.response.status())
      }

      Then("Error message is returned") {
        assertTrue(call.getBodyObject<ErrorMessage>().message.isNotBlank())
      }
    }

    Scenario("An account exists") {
      val testEngine = buildTestEngine(true)
      lateinit var call: TestApplicationCall
      lateinit var testUser: User
      lateinit var testAccount: Account
      lateinit var testAccount2: Account

      Given("A user exists") {
        testUser = testEngine.createTestUser()
      }

      Given("A user has accounts") {
        testAccount = testEngine.createTestAccount(testUser.id)
        testAccount2 = testEngine.createTestAccount(testUser.id)
      }

      When("Deleting a account") {
        call = testEngine.handleRequest(HttpMethod.Delete, "/v1/accounts/${testAccount.id}")
      }

      Then("Response status is 204") {
        assertEquals(HttpStatusCode.NoContent, call.response.status())
      }

      Then("Only a single account is deleted") {
        val accountService: AccountService by testEngine.dependency()
        val actualAccounts = runBlocking { accountService.getAll() }
        assertTrue(actualAccounts.none { it.id == testAccount.id })
        assertTrue(actualAccounts.any { it.id == testAccount2.id })
      }
    }
  }
})