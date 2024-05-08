package pl.msitko.realworld.client

import pl.msitko.realworld.endpoints.HealthEndpoint
import sttp.client3.{HttpClientSyncBackend, UriContext}
import sttp.tapir.client.sttp.SttpClientInterpreter

object Client:
  def main(args: Array[String]): Unit =
    val backend = HttpClientSyncBackend()
    val baseUri = Some(uri"http://localhost:8080")

    val clientInterpreter = SttpClientInterpreter.apply()
    val req               = clientInterpreter.toRequest(HealthEndpoint.health, baseUri)
    val response          = req(()).send(backend)

    println(response)
