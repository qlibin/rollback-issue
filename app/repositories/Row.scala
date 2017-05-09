package repositories

import java.time.Instant

import play.api.libs.json.Json

case class Row(id: Long, text: String, created: Instant, created_by: String)

object Row {
  implicit val rowFormat = Json.format[Row]
}