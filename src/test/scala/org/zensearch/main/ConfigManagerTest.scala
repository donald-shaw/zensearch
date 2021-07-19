package org.zensearch.main

import akka.testkit.TestActorRef

import org.zensearch.ZenSearchTestBase
import org.zensearch.main.ConfigMessages.GetTypeInfo
import org.zensearch.main.ConfigModel.{CrossLink, TypeInfo, TypeInfos}
import org.zensearch.model.Model.DataType

trait ConfigManagerTestBase extends ZenSearchTestBase {

  val dtype1 = DataType("us")
  val dtype2 = DataType("tic")
  val info1 = TypeInfo("us", "test1.json",
                       CrossLink("org", "t1_id", "test2s", "link_id",
                                 "misc" :: Nil, "test1", "t1_id" :: "name" :: Nil, Some(true)) ::
                       CrossLink("tic", "assoc_t3_id", "assoc_t3", "t3_id",
                                 "t3_id" :: "name" :: Nil, "test1s", "t1_id" :: "name" :: Nil, None) :: Nil)
  val info2 = TypeInfo("org", "test2.json",
                       CrossLink("us", "link_id", "test1", "t1_id",
                                 "t1_id" :: "name" :: Nil, "test2s", "misc" :: Nil, None) :: Nil)
  val info3 = TypeInfo("tic", "test3.json",
                       CrossLink("us", "t3_id", "test1s", "assoc_t3_id",
                                 "t1_id" :: "name" :: Nil, "assoc_t3", "t3_id" :: "name" :: Nil, Some(true)) :: Nil)
  val expectedInfos = TypeInfos(info1 :: info2 :: info3 :: Nil)
}

class ConfigManagerTest extends ConfigManagerTestBase {

  test("Config Manager should parse configuration data and pass back type info upon request") {

    val cfgMgr = TestActorRef(ConfigManager.props)

    cfgMgr ! GetTypeInfo

    expectMsgPF() {
      case typeInfo: TypeInfos =>
        assert(typeInfo == expectedInfos)
    }
  }
}
