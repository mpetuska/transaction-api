package lt.petuska.transaction.api.routing.accounts.account

import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import lt.petuska.transaction.api.domain.*
import lt.petuska.transaction.api.domain.cex.*
import lt.petuska.transaction.api.domain.error.*
import lt.petuska.transaction.api.model.*
import lt.petuska.transaction.api.testutil.*
import org.spekframework.spek2.*
import org.spekframework.spek2.style.gherkin.*
import kotlin.test.*

@KtorExperimentalAPI
object AccountBalanceRouteFeature : Spek({
  Feature("Account balance") {
    Scenario("No account exist") {
      val testEngine = buildTestEngine(true)
      lateinit var call: TestApplicationCall

      When("Getting account balance") {
        call = testEngine.handleRequest(HttpMethod.Get, "/v1/accounts/5/balance")
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

      Given("A user has accounts with some sweet cash") {
        testAccount = testEngine.createTestAccount(testUser.id, startingBalance = 420.69)
        testEngine.createTestAccount(testUser.id)
      }

      When("Getting account balance") {
        call = testEngine.handleRequest(HttpMethod.Get, "/v1/accounts/${testAccount.id}/balance")
      }

      Then("Response status is 200") {
        assertEquals(HttpStatusCode.OK, call.response.status())
      }

      Then("Account balance is returned") {
        val expected = FieldResponse(
          "balance",
          CexCurrency(420.69, testAccount.currency)
        ).toJsonAndBack()

        assertEquals(
          expected,
          call.getBodyObject()
        )
      }
    }
  }
})