package pl.msitko.realworld

import cats.data.EitherT
import cats.effect.IO
import sttp.tapir.Endpoint
import sttp.tapir.server.PartialServerEndpoint
import sttp.tapir.server.ServerEndpoint.Full

package object wiring:
  extension [A, Req, Err, Resp](endpoint: Endpoint[A, Req, Err, Resp, Any])
    def resultLogic(fn: Req => EitherT[IO, Err, Resp])(implicit
        aIsUnit: A =:= Unit): Full[Unit, Unit, Req, Err, Resp, Any, IO] =
      endpoint.serverLogic(fn.andThen(_.value))

  extension [Ctx, Req, Err, Resp](endpoint: PartialServerEndpoint[_, Ctx, Req, Err, Resp, Any, IO])
    def resultLogic(fn: Ctx => Req => EitherT[IO, Err, Resp]) =
      endpoint.serverLogic(ctx => req => fn(ctx)(req).value)
