package lt.petuska.transaction.api.routing.users.user

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
object GetUserRouteFeature : Spek({
  Feature("Get user") {
    Scenario("No user exist") {
      val testEngine = buildTestEngine()
      lateinit var call: TestApplicationCall

      When("Retrieving a user") {
        call = testEngine.handleRequest(HttpMethod.Get, "/v1/users/1")
      }

      Then("Response status is 404") {
        assertEquals(HttpStatusCode.NotFound, call.response.status())
      }

      Then("Error message is returned") {
        assertTrue(call.getBodyObject<ErrorMessage>().message.isNotBlank())
      }
    }

    Scenario("A user exists") {
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

      When("Retrieving a user") {
        call = testEngine.handleRequest(HttpMethod.Get, "/v1/users/${users[0].id}")
      }

      Then("Response status is 200") {
        assertEquals(HttpStatusCode.OK, call.response.status())
      }

      Then("A correct user is returned") {
        assertEquals(users[0], call.getBodyObject())
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
      When("Retrieving a user") {
        call = mockTestEngine.handleRequest(HttpMethod.Get, "/v1/users/2")
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