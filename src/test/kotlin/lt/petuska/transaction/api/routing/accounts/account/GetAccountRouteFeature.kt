package lt.petuska.transaction.api.routing.accounts.account

import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import lt.petuska.transaction.api.domain.error.*
import lt.petuska.transaction.api.model.*
import lt.petuska.transaction.api.testutil.*
import org.spekframework.spek2.*
import org.spekframework.spek2.style.gherkin.*
import kotlin.test.*

@KtorExperimentalAPI
object GetAccountRouteFeature : Spek({
  Feature("Get account") {
    Scenario("No account exist") {
      val testEngine = buildTestEngine(true)
      lateinit var call: TestApplicationCall

      When("Retrieving an account") {
        call = testEngine.handleRequest(HttpMethod.Get, "/v1/accounts/1")
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

      Given("A user exists") {
        testUser = testEngine.createTestUser()
      }

      Given("A user has accounts") {
        testAccount = testEngine.createTestAccount(testUser.id)
        testEngine.createTestAccount(testUser.id)
      }

      When("Getting an account") {
        call = testEngine.handleRequest(HttpMethod.Get, "/v1/accounts/${testAccount.id}")
      }

      Then("Response status is 200") {
        assertEquals(HttpStatusCode.OK, call.response.status())
      }

      Then("A correct account is returned") {
        assertEquals(testAccount, call.getBodyObject())
      }
    }
  }
})