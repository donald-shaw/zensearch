package org.zensearch.model

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import spray.json._

import org.zensearch.main.ConfigModel.CrossLink

object Model {

  case class DataType(name: String)

  case class InternalId(dtype: DataType, id: Int)

  case class DataEntry(id: InternalId, json: JsObject)

  case class Criteria(dtype: DataType, field: String)

  case class RequestId(id: Int)

  case class Request(id: RequestId, startTime: LocalDateTime, criteria: Criteria, value: Option[String])

   object Request {
     private var reqCount = 0
     def apply(criteria: Criteria, value: Option[String]): Request = {
       reqCount += 1
       Request(RequestId(reqCount), LocalDateTime.now, criteria, value)
     }
   }

  case class Response(request: Request, endTime: LocalDateTime, duration_ms: Long,
                      output: List[DataEntry], crossLinked: List[(Response, CrossLink)]) {

    def addCrossLinks(crossLinks: List[(Response, CrossLink)]) = copy(crossLinked = crossLinks)
  }

  object Response {
    def apply(request: Request, output: List[DataEntry]): Response = {
      val endTime = LocalDateTime.now
      Response(request, endTime, request.startTime.until(endTime, ChronoUnit.MICROS), output, Nil)
    }
  }

  case class LinkEntry(dtype: DataType, refField: Option[String], output: List[OutputEntry])

  case class FormattedData(text: String, links: List[LinkEntry])

  case class OutputEntry(data: DataEntry, text: FormattedData)

  case class FormattedResponse(request: Request, endTime: LocalDateTime, duration_ms: Long, output: List[OutputEntry])

}
