package lt.petuska.transaction.api.routing.accounts

import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import lt.petuska.transaction.api.model.*
import lt.petuska.transaction.api.testutil.*
import org.spekframework.spek2.*
import org.spekframework.spek2.style.gherkin.*
import kotlin.test.*

@KtorExperimentalAPI
object GetAllAccountsRouteFeature : Spek({
  Feature("Get all accounts") {
    Scenario("No accounts exist") {
      val testEngine = buildTestEngine()
      lateinit var call: TestApplicationCall

      When("Retrieving all accounts") {
        call = testEngine.handleRequest(HttpMethod.Get, "/v1/accounts")
      }

      Then("Response status is 200") {
        assertEquals(HttpStatusCode.OK, call.response.status())
      }

      Then("An empty list is returned") {
        assertEquals(listOf<User>(), call.getBodyObject())
      }
    }

    Scenario("Multiple accounts exist") {
      val testEngine = buildTestEngine(true)
      lateinit var call: TestApplicationCall
      lateinit var testUserA: User
      lateinit var accountsA: List<Account>
      lateinit var testUserB: User
      lateinit var accountsB: List<Account>

      Given("A user A exists") {
        testUserA = testEngine.createTestUser()
      }

      And("A user A has accounts") {
        accountsA = mutableListOf<Account>().apply {
          repeat(5) { i ->
            testEngine.createTestAccount(testUserA.id, "John Doe A $i").also { add(it) }
          }
        }
      }

      Given("A user B exists") {
        testUserB = testEngine.createTestUser()
      }

      And("A user B has accounts") {
        accountsB = mutableListOf<Account>().apply {
          repeat(5) { i ->
            testEngine.createTestAccount(testUserB.id, "John Doe B $i").also { add(it) }
          }
        }
      }

      When("Retrieving all accounts") {
        call = testEngine.handleRequest(HttpMethod.Get, "/v1/accounts")
      }

      Then("Response status is 200") {
        assertEquals(HttpStatusCode.OK, call.response.status())
      }

      Then("All accounts are returned") {
        assertEquals(accountsA + accountsB, call.getBodyList<Account>())
      }
    }
  }
})