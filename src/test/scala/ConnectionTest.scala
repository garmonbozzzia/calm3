/* Created on 12.12.17 */
import Inits._
import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import ammonite.ops.{pwd, _}
import net.ruippeixotog.scalascraper.model.Element
import org.gzb.utils.Core._
import utest._

import scala.concurrent.Future

object CourseLines {
  def apply(filePath: Path): CourseLines = new CourseLines(read(filePath) |> Parsers.allCourses)
  def apply(): Future[CourseLines] = ReqestQueue(CalmRequests.allCourses)
    .flatMap(x => Unmarshal(x.entity).to[String])
    .map(Parsers.allCourses)
    .map(CourseLines(_))
}

case class CourseLines(data: List[CourseLine]) {
  lazy val actives: CourseLines = data.filterNot(x => List(" ", "7").contains(x.type_))
    .filterNot(_.location == "TEST") |> (CourseLines(_))
  lazy val inactives: CourseLines = data.diff(actives.data) |> (CourseLines(_))
  lazy val c10d: CourseLines = data.filter(_.type_ == "0") |> (CourseLines(_))
  lazy val children: CourseLines = data.filter(_.type_ == "1") |> (CourseLines(_))
  lazy val other: CourseLines = data.diff(c10d.data) |> (CourseLines(_))
//  lazy val other: CourseLines = data.filterNot(x => List("0").contains( x.type_ == _)) |> (CourseLines(_))
  lazy val categories = List(
    data.map(_.location).distinct,
    data.map(_.status).distinct,
    data.map(_.type_).distinct,
    data.map(_.description).distinct
  )
}
case class CourseLine(id: Int, dateFrom: String, location: String, status: String, type_ : String, description: String, tat: String, registers: String )

object Parsers {
  import net.ruippeixotog.scalascraper.browser.JsoupBrowser
  import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
  import net.ruippeixotog.scalascraper.dsl.DSL._
  val browser = JsoupBrowser()

  def parseCourseLine(element: Element): CourseLine = element.children.map(_.text).toList.filter(_.nonEmpty) match {
      case List(_, dateFrom, dateTo, location, status, type_, description, tat, _, _, _, _, _, id, _, registers) =>
        CourseLine(id.toInt, dateFrom, location, status, type_, description, tat, registers)
    }

  def allCourses(html: String): List[CourseLine] = {
    val cs = browser.parseString(html) >> element("#COURSES") >> elementList("tbody tr")
    cs.filter(_.hasAttr("id"))
        .traceWith(_.length)
      .map(parseCourseLine(_))
//        .traceWith(categories(_).mkString("\n"))
//      .mkString("\n")
  }
}

//todo rename
object ConnectionTest extends TestSuite {

  val tests = Tests {
    * - {
      ReqestQueue(CalmRequests.allCourses).map(_ <| (x => assert(x.status == StatusCodes.OK)))
      //  .map(_ <| (_.entity.discardBytes()))
        .flatMap(x => Unmarshal(x.entity).to[String])
        .map(Parsers.allCourses)
    }
    'ParseTests {
      def categories(courses: List[CourseLine]) = {
        List(
          courses.map(_.location).distinct,
          courses.map(_.status).distinct,
          courses.map(_.type_).distinct,
          courses.map(_.description).distinct
        )
      }
      val allCoursesTestPath = pwd/'src/'test/'resources/"AllCourses.html"
      * - {
//        ReqestQueue(CalmRequests.allCourses).map(_ <| (x => assert(x.status == StatusCodes.OK)))
//          //  .map(_ <| (_.entity.discardBytes()))
//          .flatMap(x => Unmarshal(x.entity).to[String])
//          .map(write(allCoursesTestPath, _))
//        val allCourses = read(allCoursesTestPath) |> Parsers.allCourses
        val courseLines = CourseLines(allCoursesTestPath)
//        val cats = categories(courseLines.allCourses).traceWith(_.mkString("\n"))
        courseLines.inactives.data
          //.traceWith(_.mkString("\n"))
          .length ==> 113
        courseLines.actives.data.length ==> 288
        courseLines.data.count(_.type_ == " 7") ==> 0
        courseLines.data.count(_.type_ == "7") ==> 60
//        categories(courseLines.inactives).traceWith(_.mkString("\n"))
//        categories(courseLines.actives).traceWith(_.mkString("\n"))
        courseLines.actives.c10d.data.length <== 151
        courseLines.actives.c10d.categories.trace
        courseLines.actives.other
          .traceWith(_.data.mkString("\n"))
          .traceWith(_.data.length).categories.trace
//        courseLines.inactives.c10d.traceWith(_.data.length)
      }
    }
    // ( , 7,  ,  ,  ,  , G,  , ,  )
    // (0,  , 3, 4, 2,  ,  , 1, A, F)

  }
  override def utestAfterAll(): Unit =
    Http().shutdownAllConnectionPools()
      .onComplete { _ =>
        materializer.shutdown()
        system.terminate()
      }
}