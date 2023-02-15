package pl.msitko.realworld.services

import cats.effect.IO
import com.dimafeng.testcontainers.PostgreSQLContainer
import munit.CatsEffectSuite
import com.dimafeng.testcontainers.munit.fixtures.TestContainersFixtures
import doobie.Transactor
import org.testcontainers.utility.DockerImageName
import pl.msitko.realworld.Entities.{
  Articles,
  CreateArticleReq,
  CreateArticleReqBody,
  RegistrationReqBody,
  RegistrationUserBody
}
import pl.msitko.realworld.{DBMigration, JwtConfig}

import scala.concurrent.duration.*

class ArticleServiceSpec extends CatsEffectSuite with TestContainersFixtures {
  val postgres = new ForAllContainerFixture(PostgreSQLContainer(DockerImageName.parse("postgres:15-alpine"))) {
    override def afterContainerStart(container: PostgreSQLContainer): Unit = {
      super.afterContainerStart(container)

      container.jdbcUrl

      DBMigration.migrate(container.jdbcUrl, container.username, container.password).unsafeRunSync()
    }
  }

  override def munitFixtures = List(postgres)

  test("Feed should return multiple articles created by followed users, ordered by most recent first") {
    val transactor = createTransactor(postgres())
    val repos      = Repos.fromTransactor(transactor)

    val articleService = new ArticleService(repos.articleRepo, repos.commentRepo, repos.followRepo)
    val followService  = new ProfileService(repos.followRepo, repos.userRepo)
    val userService    = new UserService(repos.userRepo, jwtConfig)

    for {
      t  <- userService.registration(registrationReqBody("user1"))
      t2 <- userService.registration(registrationReqBody("user2"))
      t3 <- userService.registration(registrationReqBody("user3"))
      (user1Id, user2Id, user3Id) = (t._1.id, t2._1.id, t3._1.id)
      _     <- articleService.createArticle(user1Id)(createArticleReqBody("title1"))
      _     <- articleService.createArticle(user2Id)(createArticleReqBody("title2"))
      _     <- articleService.createArticle(user3Id)(createArticleReqBody("title3"))
      _     <- articleService.createArticle(user3Id)(createArticleReqBody("title4"))
      _     <- articleService.createArticle(user3Id)(createArticleReqBody("title5"))
      feed1 <- articleService.feedArticles(user2Id)
      _     <- IO(assertEquals(feed1, Articles(List.empty)))
      _     <- followService.followProfile(user2Id)("user3")
      feed2 <- articleService.feedArticles(user2Id)
      _     <- IO(assertEquals(feed2.articles.map(_.title), List("title5", "title4", "title3")))
    } yield ()

  }

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

  val jwtConfig = JwtConfig(
    secret = "abc",
    expiration = 1.day
  )

}
