package org.zensearch.main

import akka.actor.{Actor, ActorLogging, Props}
import akka.event.LoggingReceive
import com.typesafe.config.{ConfigFactory}
import scala.io.Source
import spray.json._
import spray.json.DefaultJsonProtocol._

object ConfigMessages {
  case class GetValues(keys: Set[String])
  case object GetTypeInfo
}

object ConfigModel {
  case class CrossLink(type_name: String, ref_field: String, display_name: String, linked_field: String,
                       display_from_linked: List[String], back_link_display_name: String,
                       back_link_display: List[String], reverse: Option[Boolean] = Some(false)) {

    def flip(flip_type: String) = CrossLink(flip_type, linked_field, back_link_display_name, ref_field,
                                            back_link_display, display_name, display_from_linked, Some(true))
  }
  object CrossLink { implicit val f = jsonFormat8(CrossLink.apply) }

  case class TypeInfos(infos: List[TypeInfo])
  case class TypeInfo(type_name: String, file_name: String, cross_links: List[CrossLink])
  object TypeInfo { implicit val f = jsonFormat3(TypeInfo.apply) }
}

class ConfigManager() extends Actor with ActorLogging {

  import ConfigMessages._
  import ConfigModel._

  val conf = ConfigFactory.load

  def parseTypeInfo = {
    log.debug(s"parsing type info")
    val typeInfoFileName = conf.getString("zensearch.types.info")
    val typeEntries = Source.fromInputStream(getClass.getResourceAsStream(s"/$typeInfoFileName"))
                            .getLines().mkString("\n")
    typeEntries.parseJson match {
      case JsArray(vals) =>
        val typeInfos = vals.map(_.convertTo[TypeInfo]).toList
        val flippedLinks = for {
            info <- typeInfos
            xlink <- info.cross_links
          } yield (xlink.type_name -> xlink.flip(info.type_name))
        val flippedLinksByType = flippedLinks.groupBy(_._1).mapValues(_.map(_._2))
        typeInfos.map { info =>
          info.copy(cross_links = flippedLinksByType.getOrElse(info.type_name, Nil) ++ info.cross_links)
        } // Add reversed cross-links into respective opposite type's info
      case _ =>
        log.error(s"Cannot read configuration - type-info data doesn't read as a Json array")
        Thread.sleep(500) // Wait half a sec to give chance for log msg to pass through
        context.system.terminate() // Shutdown the system!
        Nil // Just to keep the type-checking happy
    }
  }

  val typeInfo = TypeInfos(parseTypeInfo)

  def receive = LoggingReceive {
    case GetTypeInfo =>
      sender ! typeInfo
  }

}

object ConfigManager {
  def props = Props(classOf[ConfigManager])
}
