package lt.petuska.transaction.api.routing.users

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
object GetAllUsersRouteFeature : Spek({
  Feature("Get all users") {
    Scenario("No users exist") {
      val testEngine = buildTestEngine()
      lateinit var call: TestApplicationCall

      When("Retrieving all users") {
        call = testEngine.handleRequest(HttpMethod.Get, "/v1/users")
      }

      Then("Response status is 200") {
        assertEquals(HttpStatusCode.OK, call.response.status())
      }

      Then("An empty list is returned") {
        assertEquals(listOf<User>(), call.getBodyObject())
      }
    }

    Scenario("Multiple users exist") {
      val testEngine = buildTestEngine()
      lateinit var call: TestApplicationCall
      lateinit var users: List<User>

      Given("Database contains users") {
        users = mutableListOf<User>().apply {
          repeat(5) { i ->
            testEngine.createTestUser("John Doe $i").also { add(it) }
          }
        }
      }

      When("Retrieving all users") {
        call = testEngine.handleRequest(HttpMethod.Get, "/v1/users")
      }

      Then("Response status is 200") {
        assertEquals(HttpStatusCode.OK, call.response.status())
      }

      Then("All users are returned") {
        assertEquals(users, call.getBodyList())
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