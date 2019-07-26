package lt.petuska.transaction.api.routing.users.user.accounts

import com.zaxxer.hikari.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import io.mockk.*
import lt.petuska.transaction.api.config.*
import lt.petuska.transaction.api.domain.error.*
import lt.petuska.transaction.api.model.*
import lt.petuska.transaction.api.testutil.*
import org.jetbrains.exposed.sql.*
import org.kodein.di.generic.*
import org.spekframework.spek2.*
import org.spekframework.spek2.style.gherkin.*
import kotlin.test.*

@KtorExperimentalAPI
object GetAllAccountsRouteFeature : Spek({
  Feature("Get all user's accounts") {
    Scenario("User has no accounts") {
      val testEngine = buildTestEngine()
      lateinit var call: TestApplicationCall
      lateinit var testUser: User

      Given("A user exists") {
        testUser = testEngine.createTestUser()
      }

      When("Retrieving all user's accounts") {
        call = testEngine.handleRequest(HttpMethod.Get, "/v1/users/${testUser.id}/accounts")
      }

      Then("Response status is 200") {
        assertEquals(HttpStatusCode.OK, call.response.status())
      }

      Then("An empty list is returned") {
        assertEquals(listOf<User>(), call.getBodyList())
      }
    }

    Scenario("User has multiple accounts") {
      val testEngine = buildTestEngine(true)
      lateinit var call: TestApplicationCall
      lateinit var testUser: User
      lateinit var accounts: List<Account>

      Given("A user exists") {
        testUser = testEngine.createTestUser()
      }

      And("A user has multiple accounts") {
        accounts = mutableListOf<Account>().apply {
          repeat(5) { i ->
            testEngine.createTestAccount(testUser.id, name = "Test user account #$i").also { add(it) }
          }
        }
      }

      When("Retrieving all user's accounts") {
        call = testEngine.handleRequest(HttpMethod.Get, "/v1/users/${testUser.id}/accounts")
      }

      Then("Response status is 200") {
        assertEquals(HttpStatusCode.OK, call.response.status())
      }

      Then("All users are returned") {
        assertEquals(accounts, call.getBodyList())
      }
    }

    Scenario("Database error") {
      val kodeinOverrides = kodeinConfig {
        registerBindings()
        bind<Database>("main", true) with singleton {
          spyk(Database.connect(instance<HikariDataSource>("main")))
        }
      }
      val mockTestEngine = buildTestEngine(false, kodeinOverrides)

      Given("Database is misbehaving") {

      }
      lateinit var call: TestApplicationCall
      When("Retrieving all users") {
        call = mockTestEngine.handleRequest(HttpMethod.Get, "/v1/users")
      }

      Then("Response status is 502") {
        assertEquals(HttpStatusCode.BadGateway, call.response.status())
      }

      Then("Error message is returned") {
        assertTrue(call.getBodyObject<ErrorMessage>().message.isNotBlank())
      }
    }
  }
})