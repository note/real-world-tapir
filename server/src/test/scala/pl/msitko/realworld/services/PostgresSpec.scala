package pl.msitko.realworld.services

import cats.data.EitherT
import cats.effect.IO
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.munit.fixtures.TestContainersFixtures
import doobie.Transactor
import munit.{CatsEffectSuite, Location}
import org.testcontainers.utility.DockerImageName
import pl.msitko.realworld.entities.{CreateArticleReq, CreateArticleReqBody, RegistrationReqBody, RegistrationUserBody}
import pl.msitko.realworld.db.Pagination
import pl.msitko.realworld.{DBMigration, JwtConfig}

import scala.concurrent.duration.*

trait PostgresSpec extends CatsEffectSuite with TestContainersFixtures:
  private val imageName = DockerImageName.parse("postgres:15-alpine")
  val postgres = new ForAllContainerFixture(PostgreSQLContainer(imageName)) {
    override def afterContainerStart(container: PostgreSQLContainer): Unit = {
      super.afterContainerStart(container)

      container.jdbcUrl

      DBMigration.migrate(container.jdbcUrl, container.username, container.password).unsafeRunSync()
      ()
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

  def createArticleReqBody(title: String, tags: List[String] = List.empty) =
    CreateArticleReqBody(
      CreateArticleReq(
        title = title,
        description = "some descripion",
        body = "some body",
        tagList = tags
      )
    )

  def createTransactor(c: PostgreSQLContainer): Transactor[IO] =
    Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",
      url = c.jdbcUrl,
      user = c.username,
      password = c.password,
      logHandler = None
    )

  protected def ioAssertEquals[A, B, E](
      obtained: A,
      expected: B,
      clue: => Any = "values are not the same"
  )(implicit loc: Location, ev: B <:< A): EitherT[IO, E, Unit] =
    EitherT(IO.apply(Right[E, Unit](assertEquals(obtained, expected, clue))))
