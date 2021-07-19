package org.zensearch.util

import akka.actor.{Actor, PoisonPill}
import scala.concurrent.duration.FiniteDuration

trait ShortLivedActor extends Actor {

  def max_time_to_live: FiniteDuration

  override def preStart {
    import context.dispatcher
    context.system.scheduler.scheduleOnce(max_time_to_live, self, PoisonPill)
  }

}
