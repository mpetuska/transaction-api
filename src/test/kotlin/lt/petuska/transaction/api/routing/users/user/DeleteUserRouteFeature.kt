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
object DeleteUserRouteFeature : Spek({
  Feature("Delete user") {
    Scenario("No user exist") {
      val testEngine = buildTestEngine()
      lateinit var call: TestApplicationCall
      When("Deleting a user") {
        call = testEngine.handleRequest(HttpMethod.Delete, "/v1/users/1")
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

      When("Deleting a user") {
        call = testEngine.handleRequest(HttpMethod.Delete, "/v1/users/${users[0].id}")
      }

      Then("Response status is 204") {
        assertEquals(HttpStatusCode.NoContent, call.response.status())
      }

      Then("Only a single user is deleted") {
        val userService: UserService by testEngine.dependency()
        val actualUsers = runBlocking { userService.getAll() }
        assertTrue(actualUsers.none { it.id == users[0].id })
        for (i in 1 until users.size) {
          assertTrue(actualUsers.any { it.id == users[i].id })
        }
      }
    }

    Scenario("A user with multiple accounts exists") {
      val testEngine = buildTestEngine(true)
      lateinit var call: TestApplicationCall
      lateinit var users: List<User>

      Given("A user exists") {
        users = mutableListOf<User>().apply {
          repeat(5) { i ->
            testEngine.createTestUser("John Doe $i").also { add(it) }
          }
        }
      }

      And("A user has multiple accounts") {
        repeat(5) { i ->
          testEngine.createTestAccount(users[0].id, "John Doe's Account #$i")
        }
      }

      When("Deleting a user") {
        call = testEngine.handleRequest(HttpMethod.Delete, "/v1/users/${users[0].id}")
      }

      Then("Response status is 204") {
        assertEquals(HttpStatusCode.NoContent, call.response.status())
      }

      Then("Only a single user is deleted") {
        val userService: UserService by testEngine.dependency()
        val actualUsers = runBlocking { userService.getAll() }
        assertTrue(actualUsers.none { it.id == users[0].id })
        for (i in 1 until users.size) {
          assertTrue(actualUsers.any { it.id == users[i].id })
        }
      }

      Then("And user's accounts are removed") {
        val userService: UserService by testEngine.dependency()
        val actualAccounts = runBlocking { userService.getAllAccounts(users[0].id) }
        assertTrue(actualAccounts.isEmpty())
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