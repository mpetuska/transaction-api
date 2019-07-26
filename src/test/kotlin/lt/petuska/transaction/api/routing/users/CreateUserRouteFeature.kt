package lt.petuska.transaction.api.routing.users

import com.zaxxer.hikari.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import io.mockk.*
import kotlinx.coroutines.*
import lt.petuska.transaction.api.config.*
import lt.petuska.transaction.api.domain.*
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
object CreateUserRouteFeature : Spek({
  Feature("Create user") {
    Scenario("User does not exist") {
      val testEngine = buildTestEngine()
      lateinit var call: TestApplicationCall

      When("Creating a user") {
        call = testEngine.handleRequest(HttpMethod.Post, "/v1/users") {
          setBodyJson(NewUser("John Doe"))
        }
      }

      Then("Response status is 201") {
        assertEquals(HttpStatusCode.Created, call.response.status())
      }

      Then("An entity is returned") {
        assertNotNull(call.getBodyObject<FieldResponse>())
      }

      Then("A user is created") {
        val userService by testEngine.dependency<UserService>()
        val user = runBlocking {
          userService.findOne { Users.id eq 1 }
        }
        assertNotNull(user)
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
      When("Creating a user") {
        call = mockTestEngine.handleRequest(HttpMethod.Post, "/v1/users") {
          setBodyJson(NewUser("John Doe"))
        }
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