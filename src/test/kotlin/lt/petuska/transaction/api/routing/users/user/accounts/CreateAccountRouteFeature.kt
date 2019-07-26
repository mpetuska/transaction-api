package lt.petuska.transaction.api.routing.users.user.accounts

import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import kotlinx.coroutines.*
import lt.petuska.transaction.api.domain.*
import lt.petuska.transaction.api.domain.enumeration.*
import lt.petuska.transaction.api.model.*
import lt.petuska.transaction.api.service.*
import lt.petuska.transaction.api.testutil.*
import org.spekframework.spek2.*
import org.spekframework.spek2.style.gherkin.*
import kotlin.test.*

@KtorExperimentalAPI
object CreateAccountRouteFeature : Spek({
  Feature("Create account") {
    Scenario("User has no accounts") {
      val testEngine = buildTestEngine(true)
      lateinit var call: TestApplicationCall
      lateinit var testUser: User

      Given("A user exists") {
        testUser = testEngine.createTestUser()
      }

      When("Creating an account") {
        call = testEngine.handleRequest(HttpMethod.Post, "/v1/users/${testUser.id}/accounts") {
          setBodyJson(NewAccount("John Doe's account", Currency.EUR))
        }
      }

      Then("Response status is 201") {
        assertEquals(HttpStatusCode.Created, call.response.status())
      }

      Then("An entity is returned") {
        assertNotNull(call.getBodyObject<FieldResponse>())
      }

      Then("An account is created") {
        val userService by testEngine.dependency<UserService>()
        val accounts = runBlocking {
          userService.getAllAccounts(testUser.id)
        }
        assertTrue(accounts.size == 1)
      }
    }

    Scenario("User already has an account") {
      val testEngine = buildTestEngine(true)
      lateinit var call: TestApplicationCall
      lateinit var testUser: User
      lateinit var existingAccount: Account

      Given("A user exists") {
        testUser = testEngine.createTestUser()
      }

      And("A user has an account") {
        existingAccount = testEngine.createTestAccount(testUser.id)
      }

      When("Creating an account") {
        call = testEngine.handleRequest(HttpMethod.Post, "/v1/users/${testUser.id}/accounts") {
          setBodyJson(NewAccount("John Doe's account", Currency.EUR))
        }
      }

      Then("Response status is 201") {
        assertEquals(HttpStatusCode.Created, call.response.status())
      }

      Then("An entity is returned") {
        assertNotNull(call.getBodyObject<FieldResponse>())
      }

      Then("An account is created") {
        val userService by testEngine.dependency<UserService>()
        val accounts = runBlocking {
          userService.getAllAccounts(testUser.id)
        }
        assertTrue(accounts.any { it.id != existingAccount.id })
      }

      Then("An existing account is not gone") {
        val userService by testEngine.dependency<UserService>()
        val accounts = runBlocking {
          userService.getAllAccounts(testUser.id)
        }
        assertTrue(accounts.any { it.id == existingAccount.id })
      }

    }
  }
})