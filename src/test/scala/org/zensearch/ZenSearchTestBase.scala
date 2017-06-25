package org.zensearch

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKitBase}
import org.scalatest.{BeforeAndAfterAll, FunSuite}

trait ZenSearchTestBase extends FunSuite with TestKitBase with ImplicitSender with BeforeAndAfterAll {

  implicit lazy val system = ActorSystem("ZenSearchTest")
  lazy val log = system.log

  override protected def afterAll(): Unit = {
    system.terminate
    super.afterAll()
  }
}
