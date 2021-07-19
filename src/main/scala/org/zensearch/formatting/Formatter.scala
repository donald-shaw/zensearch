package org.zensearch.formatting

import spray.json.{JsArray, JsObject, JsValue}

import org.zensearch.main.ConfigModel.CrossLink
import org.zensearch.messages.Messages.FormatResult
import org.zensearch.model.Model.{DataEntry, FormattedData, OutputEntry, Response}

trait Formatter {
  def formatResponse(baseResp: Response): FormatResult
}

trait FormatterFactory {
  def apply(): Formatter
}

object DefaultFormatterFactory extends FormatterFactory {
  def apply() = new BasicJsonFormatter()
}

class BasicJsonFormatter extends Formatter {
  
  override def formatResponse(baseResp: Response) =
      FormatResult(baseResp.output map { entry =>
        val matchedLinks = baseResp.crossLinked.filter(_._1.id == entry.id)
        OutputEntry(entry, FormattedData(addLinks(entry.json, matchedLinks).prettyPrint, Map.empty))
      })
  
  private def addLinks(base: JsObject, linked: List[(DataEntry, Response, CrossLink)]) = {
    val linkFields = for {
      (link, xlinkData) <- linked.flatMap{ case (entry, resp, xlink) => resp.output.map(_ -> xlink) }.toSet

      jsonValue = if (xlinkData.display_from_linked.size == 1) {
        link.json.fields.find{ case ((name, value)) => xlinkData.display_from_linked.head == name }.map(_._2).get
      } else {
        JsObject(link.json.fields.filterKeys(xlinkData.display_from_linked.contains).toSeq: _*)
      }
    } yield (xlinkData -> (jsonValue: JsValue))
    val extra = linkFields.groupBy(_._1).mapValues(_.map(_._2)).flatMap { case (xlinkData,  values) =>
      if (xlinkData.reverse.getOrElse(false)) {
        Some(xlinkData.display_name -> JsArray(values.toList))
      } else { // expect only one, so take the head (if it exists)
        values.headOption map { value => xlinkData.display_name -> value }
      }
    }
    JsObject(base.fields ++ extra)
  }
}
