package lt.petuska.transaction.api.config

import com.zaxxer.hikari.*
import io.ktor.application.*
import io.ktor.util.*
import lt.petuska.transaction.api.model.*
import lt.petuska.transaction.api.service.*
import lt.petuska.transaction.api.util.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*
import org.kodein.di.*
import org.kodein.di.generic.*

@KtorExperimentalAPI
fun Kodein.Builder.registerBindings() {
  bind<HikariDataSource>("main") with singleton {
    instance<Application>().run {
      HikariConfig().apply {
        driverClassName = getAppProperty("db.driver")
        jdbcUrl = getAppProperty("db.jdbcUrl")
        maximumPoolSize = getAppProperty("db.maximumPoolSize")!!
        isAutoCommit = getAppProperty("db.isAutoCommit")
        username = getAppProperty("db.username")
        password = getAppProperty("db.password")
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
      }
    }.let { HikariDataSource(it) }

  }
  bind<Database>("main") with singleton {
    Database.connect(instance<HikariDataSource>("main")).also {
      transaction(it) {
        SchemaUtils.create(Users)
        SchemaUtils.create(Accounts)
        SchemaUtils.create(Transactions)
      }
    }
  }
  bind<UserService>() with singleton { UserService(instance("main"), instance(), instance("rootUserName")) }
  bind<AccountService>() with singleton { AccountService(instance("main"), instance(), instance("rootAccountName")) }
  bind<AccountCheckService>() with singleton { AccountCheckService(instance("main"), instance("rootAccountName")) }
  bind<TransactionService>() with singleton { TransactionService(instance("main"), instance("revolut"), instance()) }
  bind("rootUserName") from singleton { instance<Application>().getAppProperty<String>("root.user.name") }
  bind("rootAccountName") from singleton { instance<Application>().getAppProperty<String>("root.user.account.name") }
  bind("rootAccountCurrency") from singleton { instance<Application>().getAppProperty<String>("root.user.account.currency") }
  bind("revolutApiKey") from singleton { instance<Application>().getAppProperty<String>("revolut.apiKey") }
  bind("revolutCexEndpoint") from singleton { instance<Application>().getAppProperty<String>("revolut.cexEndpoint") }
  bind<CurrencyExchangeService>("revolut") with singleton {
    CurrencyExchangeService(
      instance("revolutApiKey"),
      instance("revolutCexEndpoint")
    )
  }
}