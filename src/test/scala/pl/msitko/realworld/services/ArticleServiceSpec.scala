package pl.msitko.realworld.services

import com.dimafeng.testcontainers.PostgreSQLContainer
import munit.FunSuite
import com.dimafeng.testcontainers.munit.fixtures.TestContainersFixtures

class ArticleServiceSpec extends FunSuite with TestContainersFixtures {
  val postgres = ForAllContainerFixture(PostgreSQLContainer())

  override def munitFixtures = List(postgres)

  // TODO: rename
  test("rename") {
    println(s"bazinga ${postgres().jdbcUrl}")
    assert(postgres().jdbcUrl.nonEmpty)

  }

}
