package controllers

import fly.play.s3.{BucketFile, S3}
import play.api.Play
import play.api.Play.current
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Controller
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import java.io._

object StaticAssets extends Controller {

  val AbsolutePath = """^(/|[a-zA-Z]:\\).*""".r

  def at(file: String): Action[AnyContent] = Action.async { request =>

    current.configuration.getString("aws.accessKeyId") match {
      case Some(key) =>

        val bucket = S3("thorvarium")
        val result = bucket get file

        result.map {
          case BucketFile(name, contentType, content, acl, headers) => Ok(content).as(contentType)
        }

      case None =>

        current.configuration.getString("assets.path") match {
          case Some(assetsPath) =>

            val fileToServe = assetsPath match {
              case AbsolutePath(_) => new File(assetsPath, file)
              case _ => new File(Play.application.getFile(assetsPath), file)
            }

            scala.concurrent.Future {
              if (fileToServe.exists) {
                Ok.sendFile(fileToServe, inline = true).withHeaders(CACHE_CONTROL -> "max-age=3600")
              } else {
                NotFound
              }
            }

          case None => scala.concurrent.Future { NotFound }
        }

    }
  }
}