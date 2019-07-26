package lt.petuska.transaction.api.routing.users.user

import com.zaxxer.hikari.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import io.mockk.*
import kotlinx.coroutines.*
import lt.petuska.transaction.api.config.*
import lt.petuska.transaction.api.domain.error.*
import lt.petuska.transaction.api.model.*
import lt.petuska.transaction.api.service.*
import lt.petuska.transaction.api.testutil.*
import org.jetbrains.exposed.sql.*
import org.kodein.di.generic.*
import org.spekframework.spek2.*
import org.spekframework.spek2.style.gherkin.*
import kotlin.test.*

@KtorExperimentalAPI
object UpdateUserRouteFeature : Spek({
  Feature("Update user") {
    Scenario("No user exist") {
      val testEngine = buildTestEngine()
      lateinit var call: TestApplicationCall

      When("Updating a user") {
        call = testEngine.handleRequest(HttpMethod.Put, "/v1/users/1") {
          setBodyJson(NewUser("John Doe"))
        }
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

      val updatedUser by lazy { NewUser("${users[0].name} Updated") }
      When("Updating a user") {
        call = testEngine.handleRequest(HttpMethod.Put, "/v1/users/${users[0].id}") {
          setBodyJson(updatedUser)
        }
      }

      Then("Response status is 204") {
        assertEquals(HttpStatusCode.NoContent, call.response.status())
      }

      Then("Only a single user is updated") {
        val userService: UserService by testEngine.dependency()
        val actualUsers = runBlocking { userService.getAll() }
        actualUsers.forEach {
          val expected = if (it.id == users[0].id) {
            User(users[0].id, updatedUser.name)
          } else {
            users.find { u -> u.id == it.id }
          }
          assertEquals(it, expected)
        }
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