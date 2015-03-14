package filters

import play.api.Play
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global

class CORSFilter extends EssentialFilter {
  def apply(next: EssentialAction) = new EssentialAction {
    def apply(requestHeader: RequestHeader) = {
      val static = Play.current.configuration.getString("static.url").get
      next(requestHeader).map { result =>
        result.withHeaders("Access-Control-Allow-Origin" -> static,
          "Access-Control-Expose-Headers" -> "WWW-Authenticate, Server-Authorization",
          "Access-Control-Allow-Methods" -> "POST, GET, OPTIONS, PUT, DELETE",
          "Access-Control-Allow-Headers" -> "x-requested-with,content-type,Cache-Control,Pragma,Date")
      }
    }
  }
}