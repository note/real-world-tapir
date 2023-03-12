package pl.msitko.realworld.services

import cats.effect.IO
import pl.msitko.realworld.db.HealthRepo
import pl.msitko.realworld.endpoints.HealthResponse

object HealthService:
  def apply(repos: Repos): HealthService =
    new HealthService(repos.healthRepo)

class HealthService(healthRepo: HealthRepo):
  def getHealth: IO[HealthResponse] =
    healthRepo.testConnection().map { returned =>
      HealthResponse(
        version = pl.msitko.realworld.buildinfo.BuildInfo.version,
        available = true,
        dbAvailable = returned == 1
      )
    }
