package pl.msitko.realworld.client

import pl.msitko.realworld.endpoints.{HealthEndpoint, HealthResponse, UserEndpoints}
import sttp.client3.{HttpClientSyncBackend, Identity, Request, Response, SttpBackend, UriContext}
import sttp.model.Uri
import sttp.tapir.{DecodeResult, Endpoint, PublicEndpoint}
import sttp.tapir.client.sttp.SttpClientInterpreter

trait EndpointToRequest:
  def baseUri: Uri
  def clientInterpreter: SttpClientInterpreter

  extension [I, E, O, R](endpoint: PublicEndpoint[I, E, O, R])
    def toRequest: I => Request[DecodeResult[Either[E, O]], R] =
      clientInterpreter.toRequest(endpoint, Some(baseUri))

  extension [SI, I, E, O, R](endpoint: Endpoint[SI, I, E, O, R])
    def toSecureRequest: SI => I => Request[DecodeResult[Either[E, O]], R] =
      clientInterpreter.toSecureRequest(endpoint, Some(baseUri))

object ClientMain:
  def main(args: Array[String]): Unit =
    val baseUri = uri"http://localhost:8080"
    val httpOps = ClientOperations.Default(baseUri)

    val response = httpOps.checkHealth
    println(response)

class ClientOperations(
    override val baseUri: Uri,
    override val clientInterpreter: SttpClientInterpreter,
    backend: SttpBackend[Identity, Any])
    extends EndpointToRequest:
  def checkHealth: Response[DecodeResult[Either[Unit, HealthResponse]]] =
    HealthEndpoint.health.toRequest(()).send(backend)

object ClientOperations:
  def Default(baseUri: Uri) =
    ClientOperations(baseUri = baseUri, clientInterpreter = SttpClientInterpreter(), backend = HttpClientSyncBackend())

trait UserOperations:
  self: EndpointToRequest =>
  def authentication =
    UserEndpoints.authentication.toRequest

  def registration =
    UserEndpoints.registration.toRequest

  def getCurrentUser =
    UserEndpoints.getCurrentUser.toSecureRequest

  def updateUser =
    UserEndpoints.updateUser.toSecureRequest
