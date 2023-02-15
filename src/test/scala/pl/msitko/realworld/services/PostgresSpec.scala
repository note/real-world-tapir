package pl.msitko.realworld.services

import cats.effect.IO
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.munit.fixtures.TestContainersFixtures
import doobie.Transactor
import munit.CatsEffectSuite
import org.testcontainers.utility.DockerImageName
import pl.msitko.realworld.Entities.{CreateArticleReq, CreateArticleReqBody, RegistrationReqBody, RegistrationUserBody}
import pl.msitko.realworld.db.Pagination
import pl.msitko.realworld.{DBMigration, JwtConfig}

import scala.concurrent.duration.*

trait PostgresSpec extends CatsEffectSuite with TestContainersFixtures:
  val postgres = new ForAllContainerFixture(PostgreSQLContainer(DockerImageName.parse("postgres:15-alpine"))) {
    override def afterContainerStart(container: PostgreSQLContainer): Unit = {
      super.afterContainerStart(container)

      container.jdbcUrl

      DBMigration.migrate(container.jdbcUrl, container.username, container.password).unsafeRunSync()
    }
  }

  override def munitFixtures = List(postgres)

  val defaultPagination = Pagination(offset = 0, limit = 20)
  val jwtConfig = JwtConfig(
    secret = "abc",
    expiration = 1.day
  )

  def registrationReqBody(username: String) =
    RegistrationReqBody(
      RegistrationUserBody(
        username = username,
        email = s"$username@example.org",
        password = "abcdef",
        bio = None,
        image = None
      )
    )

  def createArticleReqBody(title: String) =
    CreateArticleReqBody(
      CreateArticleReq(
        title = title,
        description = "some descripion",
        body = "some body",
        tagList = List.empty
      )
    )

  def createTransactor(c: PostgreSQLContainer): Transactor[IO] =
    Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      c.jdbcUrl,
      c.username,
      c.password
    )
