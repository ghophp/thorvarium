package controllers

import fly.play.s3.{BucketFile, S3}
import play.api.Play.current
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Controller
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object StaticAssets extends Controller {

  def at(file: String): Action[AnyContent] = Action.async { request =>

    current.configuration.getString("aws.accessKeyId") match {
      case Some(key) =>

        val bucket = S3("thorvarium")
        val result = bucket get file

        result.map {
          case BucketFile(name, contentType, content, acl, headers) => Ok(content).as(contentType)
        }

      case None => scala.concurrent.Future { NotFound }
    }
  }
}