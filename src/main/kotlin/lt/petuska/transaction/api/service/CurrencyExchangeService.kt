package lt.petuska.transaction.api.service

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.http.*
import lt.petuska.transaction.api.domain.cex.*
import lt.petuska.transaction.api.domain.enumeration.*

class CurrencyExchangeService(
  private val apiKey: String,
  private val cexEndpoint: String
) {
  private val httpClient = HttpClient {
    install(JsonFeature) {
      serializer = GsonSerializer()
    }
  }

  suspend fun convert(amount: Double, from: Currency, to: Currency): Double {
    return if (from == to) {
      amount
    } else {
      httpClient.get<RevolutCexResponse> {
        header(HttpHeaders.Authorization, "Bearer $apiKey")
        url(cexEndpoint)
        parameter("from", from.name)
        parameter("to", to.name)
        parameter("amount", amount)
      }.to.amount
    }
  }
}