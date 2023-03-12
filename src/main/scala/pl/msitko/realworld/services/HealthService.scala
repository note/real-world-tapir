package pl.msitko.realworld.services

import cats.effect.IO
import com.typesafe.scalalogging.StrictLogging
import pl.msitko.realworld.db.HealthRepo
import pl.msitko.realworld.endpoints.HealthResponse

import scala.util.control.NonFatal

object HealthService:
  def apply(repos: Repos): HealthService =
    new HealthService(repos.healthRepo)

class HealthService(healthRepo: HealthRepo) extends StrictLogging:
  def getHealth: IO[HealthResponse] =
    healthRepo
      .testConnection()
      .map(_ == 1)
      .recover { case NonFatal(e) =>
        logger.warn(s"Error when checking DB connection: ${e}", e)
        false
      }
      .map { dbStatus =>
        HealthResponse(
          version = pl.msitko.realworld.buildinfo.BuildInfo.version,
          available = true,
          dbAvailable = dbStatus
        )
      }
