package pl.msitko.realworld.services

import cats.effect.IO
import pl.msitko.realworld.{ExampleResponses, JwtConfig}
import pl.msitko.realworld.endpoints.ProfileEndpoints

class ProfileServices(jwtConfig: JwtConfig):
  private val profileEndpoints = new ProfileEndpoints(jwtConfig)

  val getProfileImpl =
    profileEndpoints.getProfile.serverLogicSuccess { userIdOpt => profileName =>
      IO.pure(ExampleResponses.profileBody(profileName))
    }

  val followProfileImpl =
    profileEndpoints.followProfile.serverLogicSuccess { userId => profileName =>
      IO.pure(ExampleResponses.profileBody(profileName))
    }

  val unfollowProfileImpl =
    profileEndpoints.unfollowProfile.serverLogicSuccess { userId => profileName =>
      IO.pure(ExampleResponses.profileBody(profileName))
    }

  val services = List(
    getProfileImpl,
    followProfileImpl,
    unfollowProfileImpl,
  )
