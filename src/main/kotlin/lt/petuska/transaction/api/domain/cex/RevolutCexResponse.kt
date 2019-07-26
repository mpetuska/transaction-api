package lt.petuska.transaction.api.domain.cex

data class RevolutCexResponse(
  val rate: Double,
  val from: CexCurrency,
  val to: CexCurrency
)

