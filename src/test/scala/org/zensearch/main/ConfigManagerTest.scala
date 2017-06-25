package org.zensearch.main

import akka.testkit.TestActorRef

import org.zensearch.ZenSearchTestBase
import org.zensearch.main.ConfigMessages.GetTypeInfo
import org.zensearch.main.ConfigModel.{CrossLink, TypeInfo, TypeInfos}
import org.zensearch.model.Model.DataType

trait ConfigManagerTestBase extends ZenSearchTestBase {

  val dtype1 = DataType("test type 1")
  val dtype2 = DataType("test type 2")
  val info1 = TypeInfo("test1", "test1.json",
                       CrossLink("test2", "t1_id", "test2s", "link_id",
                                 "misc" :: Nil, "test1", "t1_id" :: "name" :: Nil, Some(true)) ::
                       CrossLink("test3", "assoc_t3_id", "assoc_t3", "t3_id",
                                 "t3_id" :: "name" :: Nil, "test1s", "t1_id" :: "name" :: Nil, None) :: Nil)
  val info2 = TypeInfo("test2", "test2.json",
                       CrossLink("test1", "link_id", "test1", "t1_id",
                                 "t1_id" :: "name" :: Nil, "test2s", "misc" :: Nil, None) :: Nil)
  val info3 = TypeInfo("test3", "test3.json",
                       CrossLink("test1", "t3_id", "test1s", "assoc_t3_id",
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
