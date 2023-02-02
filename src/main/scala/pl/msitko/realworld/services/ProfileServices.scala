package pl.msitko.realworld.services

import cats.effect.IO
import pl.msitko.realworld.ExampleResponses
import pl.msitko.realworld.endpoints.ProfileEndpoints

object ProfileServices:
  val getProfileImpl =
    ProfileEndpoints.getProfile.serverLogicSuccess(profileName => IO.pure(ExampleResponses.profileBody(profileName)))

  val followProfileImpl =
    ProfileEndpoints.followProfile.serverLogicSuccess(profileName => IO.pure(ExampleResponses.profileBody(profileName)))

  val unfollowProfileImpl =
    ProfileEndpoints.unfollowProfile.serverLogicSuccess(profileName =>
      IO.pure(ExampleResponses.profileBody(profileName)))

  val services = List(
    getProfileImpl,
    followProfileImpl,
    unfollowProfileImpl,
  )
