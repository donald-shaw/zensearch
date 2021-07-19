package org.zensearch.formatting

import spray.json.{JsObject, JsValue}
import System.{lineSeparator => LS}

import org.zensearch.main.ConfigModel.CrossLink
import org.zensearch.messages.Messages.FormatResult
import org.zensearch.model.Model.{DataEntry, FormattedData, OutputEntry, Request, Response}

class TextFormatter extends Formatter {
  
  val jsonFormatter = DefaultFormatterFactory()
  
  override def formatResponse(baseResp: Response) =
      FormatResult(baseResp.output map { entry =>
        OutputEntry(entry, FormattedData(addLinked(entry, baseResp), Map.empty))
      })
  
  def prefix(request: Request, entry: DataEntry) = {
    s"$LS${entry.id.dtype.name} - ${request.criteria.field} = ${request.value.map(str => s"'$str'").getOrElse("(empty)")}$LS"
  }
  
  def format(field: (String, JsValue)) = f"${field._1}%-20s${field._2}"
  
  def formatLinked(id: JsValue, fields: Map[String, JsValue], xlinkData: CrossLink) = {
    val fieldText = for {
      fname <- xlinkData.display_from_linked
      field <- fields.get(fname)
    } yield format(("    "+fname) -> field)
    f"  _id = ${id}:$LS${fieldText.mkString("",LS,"")}"
  }
  
  private def addLinked(entry: DataEntry, resp: Response) = {
    val linked: List[(DataEntry, Response, CrossLink)] = resp.crossLinked.filter(_._1.id == entry.id)
    val linkFields = for {
      (link, xlinkData) <- linked.flatMap{ case (_, resp, xlink) => resp.output.map(_ -> xlink) }.toSet
      jsonValue = if (xlinkData.display_from_linked.size == 1) {
        link.json.fields.find{ case ((name, value)) => xlinkData.display_from_linked.head == name}.map(_._2).get
      } else {
        JsObject(link.json.fields.filterKeys(xlinkData.display_from_linked.contains).toSeq: _*)
      }
    } yield (xlinkData -> (jsonValue: JsValue))
    val extra = linkFields.groupBy(_._1).mapValues(_.map(_._2)).flatMap { case (xlinkData,  values) =>
      if (xlinkData.reverse.getOrElse(false)) {
        val fields = values.toList.map(_.asJsObject.fields)
        val ids = fields.flatMap(fmap => fmap.get("_id").map(_ -> (fmap - "_id")))
        xlinkData.display_name+":" :: ids.map(inner => formatLinked(inner._1, inner._2, xlinkData))
      } else { // expect only one, so take the head (if it exists)
        values.headOption map { value => format(xlinkData.display_name -> value) }
      }
    }
    (entry.json.fields.map(format) ++ extra).mkString(prefix(resp.request, entry), LS, LS)
  }
}

object TextFormatterFactory extends FormatterFactory {
  def apply() = new TextFormatter()
}
