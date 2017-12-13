import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpRequest, Uri}
import ammonite.ops.{pwd, read}

object CalmRequests {
  implicit class CalmUri(val uri: Uri) extends AnyVal {
    def allPages = uri.withQuery( ("PAGE", "9") +: uri.query())
    def calmRequest = HttpRequest(uri = uri).addHeader(authorization)
  }
  implicit class CalmHttpRequest(val request: HttpRequest) {
    def run = ReqestQueue(request)
  }
  val authorization = RawHeader("Authorization", read(pwd/'authorization))
  val allCourses = Uri("/russia/calmx/courses/allyears").allPages.calmRequest
  case class Course(uniq: Int) {
    val quotacheck = Uri(s"/russia/calmx/courses/quotacheck?UNIQ=$uniq")
  }
}
