package pl.msitko.realworld.services

import cats.effect.IO
import pl.msitko.realworld.ExampleResponses
import pl.msitko.realworld.endpoints.UserEndpoints

object UserServices:
  val authenticationImpl =
    UserEndpoints.authentication.serverLogicSuccess(_ => IO.pure(ExampleResponses.userBody))

  val registrationImpl =
    UserEndpoints.registration.serverLogicSuccess(_ => IO.pure(ExampleResponses.userBody))

  val getCurrentUserImpl =
    UserEndpoints.getCurrentUser.serverLogicSuccess(_ => IO.pure(ExampleResponses.userBody))

  val updateUserImpl =
    UserEndpoints.updateUser.serverLogicSuccess(_ => IO.pure(ExampleResponses.userBody))

  val services = List(
    authenticationImpl,
    registrationImpl,
    getCurrentUserImpl,
    updateUserImpl,
  )
