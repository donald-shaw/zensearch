package org.zensearch.parsing

import akka.actor.{ActorRef, ActorRefFactory, Props}
import akka.testkit.{TestActorRef, TestProbe}
import scala.io.Source
import spray.json._

import org.zensearch.ZenSearchTestBase
import org.zensearch.main.ConfigModel.{TypeInfo, TypeInfos}
import org.zensearch.messages.Messages.LookupRefs
import org.zensearch.model.Model.DataType
import org.zensearch.parsing.ParserMessages.GetLookupRefs
import org.zensearch.util.ChildProvider

trait ParserManagerTestBase extends ZenSearchTestBase {

  val dtype1 = DataType("u")
  val dtype2 = DataType("t")
  val typeEntries = Source.fromInputStream(getClass.getResourceAsStream("/zentypes.json")).getLines().mkString("\n")
  val typeInfos = typeEntries.parseJson match {
    case JsArray(vals) => vals.map(_.convertTo[TypeInfo]).toList
    case _ =>
      log.error(s"Cannot parse test data frtom file 'test1.json' - data doesn't read as a Json array")
      Nil
  }

  val cfgMgrProxy = TestProbe()

  val testChildProvider = new ChildProvider {

    var ref2Use: ActorRef = cfgMgrProxy.ref

    var lastProps: Option[Props] = None

    def newChild(factory: ActorRefFactory, props: Props) = {
      lastProps = Some(props)
      ref2Use
    }
  }
}

class ParserManagerTest extends ParserManagerTestBase {

  test("Parser Manager should parse configuration data and pass back type info upon request") {

    val parser = TestActorRef(ParserManager.props(testChildProvider))

    val testFiles = typeInfos.map(_.file_name)

    parser ! GetLookupRefs(TypeInfos(typeInfos))

    expectMsgPF() {
      case refs: LookupRefs =>
        assert(refs.dataRefs.refs.size == testFiles.size)
        assert(refs.indexRefs.refs.size >= testFiles.size)
    }
  }
}
