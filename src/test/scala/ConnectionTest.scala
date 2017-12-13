/* Created on 12.12.17 */
import Inits._
import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.unmarshalling.Unmarshal
import ammonite.ops._
import net.ruippeixotog.scalascraper.model.Element
import org.gzb.utils.Core._
import utest._

object Parsers {
  import net.ruippeixotog.scalascraper.browser.JsoupBrowser
  import net.ruippeixotog.scalascraper.dsl.DSL._
  import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
  import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
  val browser = JsoupBrowser()

  def parseCourseLine(element: Element) = {
    element.children.map(_.text).filter(_.nonEmpty).size
  }

  def allCourses(html: String) = {
    val cs = browser.parseString(html) >> element("#COURSES") >> elementList("tbody tr")
    cs.map(parseCourseLine(_)).mkString("\n")
  }
}

object ConnectionTest extends TestSuite {

  val tests = Tests {
    * - {
      val authorization = RawHeader("Authorization", read(pwd/'authorization))
      val calmUri = "https://calm.dhamma-eu.org/russia/calm"
      ReqestQueue(CalmRequests.allCourses)
        .map(_ <| (x => assert(x.status == StatusCodes.OK)))
      //  .map(_ <| (_.entity.discardBytes()))
        .flatMap(x => Unmarshal(x.entity).to[String])
        .map(Parsers.allCourses)
    }
  }
  override def utestAfterAll(): Unit =
    Http().shutdownAllConnectionPools()
      .onComplete { _ =>
        materializer.shutdown()
        system.terminate()
      }
}