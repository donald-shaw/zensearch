package org.zensearch.util

import akka.actor.{ActorRef, Props, ActorRefFactory}

trait ChildProvider {
  def newChild(factory: ActorRefFactory, props: Props): ActorRef
}

object ChildProvider {
  val default = new ChildProvider {
    override def newChild(factory: ActorRefFactory, props: Props) = factory.actorOf(props)
  }
}
