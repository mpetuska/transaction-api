# A sample transaction API

## Model
The api is following the unidirectional data model as follows:
  1. Root entity: User
  2. User can have multiple accounts
  3. Each account can be associated with multiple transactions
  
## Manual Testing
For the manual testing of the API, please use the provided *transaction-api.postman_collection.json* import.
It contains a preconfigured collection of [Postman](https://www.getpostman.com/downloads/) requests with examples.

## Integration Tests
To run integration tests that are executed on Ktor's mock server engine (eliminating the need for actual HTTP requests)
execute the following command in terminal: `./gradlew test`

## Unit Tests
Not implemented

## Running the Application
To start the application from source code, execute `./gradlew run` in your terminal. 
This will start the api on port `8080`, logging at `build/logs/*.log`.

Alternatively, you can create a bootable jar via `./gradlew jar`, which will be created in `build/libs` directory 
and run it with `java -jar build/libs/*.jar`. 
This will start the api on port `8080`, logging at `logs/*.log`.

Finally, the port the application binds to can be overridden by passing in your port as `PORT` environment variable when starting the system process.

## Technology Stack
### Production
* [Ktor](https://ktor.io/) asynchronous web framework
* [Logback](https://logback.qos.ch/) logger implementation
* [Exposed](https://www.kotlinresources.com/library/exposed/) SQL framework
* [Hikari](https://brettwooldridge.github.io/HikariCP/) JDBC connection pooling library
* [H2](https://h2database.com) in-memory database
* [Kodein](https://kodein.org/) dependency injection framework

### Test
* [JUnit 5](https://junit.org/junit5/) test engine
* [Spek](https://spekframework.org/) test specification framework
* [Ktor](https://ktor.io/) server mock
* [MockK](https://mockk.io/) mocking library (UNUSED)