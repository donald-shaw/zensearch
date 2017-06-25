package org.zensearch.formatting

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}
import akka.event.LoggingReceive
import spray.json._

import org.zensearch.main.ConfigModel.CrossLink
import org.zensearch.messages.Messages.{DataStoreRefs, FormatResult}
import org.zensearch.model.Model._

class FormattingActor(refs: DataStoreRefs, requestMgr: ActorRef)
    extends Actor with ActorLogging with Stash {

  override def receive = ready

  def ready: Receive = LoggingReceive {
    case resp@Response(req, _, _, data, xLinks) =>
      finishResponse(resp, sender())

    case other =>
      log.error(s"Unexpected msg in FormattingActor - 'receive': '$other' - sender: $sender")
  }

  private def finishResponse(baseResp: Response, replyTo: ActorRef) = {

    val baseFormatted = FormatResult(baseResp.output map { entry =>
      OutputEntry(entry, FormattedData(entry.json.prettyPrint, Nil))
    })
    log.debug(s"FormattingActor - got DataResult: data = ${baseResp.output}\n\n"+
              s"Basic formatted reply: $baseFormatted, replyTo = $replyTo")
    val formatted = FormatResult(baseResp.output map { entry =>
      OutputEntry(entry, FormattedData(addLinks(entry.json, baseResp.crossLinked).prettyPrint, Nil))
    })
    log.debug(s"FormattingActor - got DataResult: data = ${baseResp.output}\n\n"+
              s"Basic formatted reply: $baseFormatted, replyTo = $replyTo"+
              s"Replying with formatted reply: $formatted, replyTo = $replyTo")
    replyTo ! formatted

  }

  private def addLinks(base: JsObject, linked: List[(Response, CrossLink)]) = {
    val linkFields = for {
      (link, xlinkData) <- linked.flatMap{ case (resp, xlink) => resp.output.map(_ -> xlink) }.toSet

      jsonValue = JsObject(link.json.fields.filterKeys(xlinkData.display_from_linked.contains).toSeq:_*)
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

object FormattingActor {
  def props(refs: DataStoreRefs, requestMgr: ActorRef) = Props(classOf[FormattingActor], refs, requestMgr)
}
