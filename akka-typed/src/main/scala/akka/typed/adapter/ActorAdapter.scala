/**
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com/>
 */
package akka.typed
package adapter

import akka.{ actor ⇒ a }

private[typed] class ActorAdapter[T](_initialBehavior: () ⇒ Behavior[T]) extends a.Actor {
  import Behavior._

  var behavior: Behavior[T] = _

  {
    behavior = canonicalize(_initialBehavior(), behavior)
    if (behavior == null) throw new IllegalStateException("initial behavior cannot be `same` or `unhandled`")
    if (!isAlive(behavior)) context.stop(self)
  }

  val ctx = new ActorContextAdapter[T](context)

  def receive = {
    case akka.actor.Terminated(ref) ⇒
      val msg = Terminated(ActorRefAdapter(ref))(null)
      next(behavior.management(ctx, msg), msg)
    case akka.actor.ReceiveTimeout ⇒
      next(behavior.management(ctx, ReceiveTimeout), ReceiveTimeout)
    case msg ⇒
      val m = msg.asInstanceOf[T]
      next(behavior.message(ctx, m), m)
  }

  private def next(b: Behavior[T], msg: Any): Unit = {
    if (isUnhandled(b)) unhandled(msg)
    behavior = canonicalize(b, behavior)
    if (!isAlive(behavior)) {
      context.stop(self)
    }
  }

  override def unhandled(msg: Any): Unit = msg match {
    case Terminated(ref) ⇒ throw new a.DeathPactException(toUntyped(ref))
    case other           ⇒ super.unhandled(other)
  }

  override val supervisorStrategy = akka.actor.SupervisorStrategy.stoppingStrategy

  override def preStart(): Unit =
    next(behavior.management(ctx, PreStart), PreStart)
  override def preRestart(reason: Throwable, message: Option[Any]): Unit =
    next(behavior.management(ctx, PreRestart), PreRestart)
  override def postRestart(reason: Throwable): Unit =
    next(behavior.management(ctx, PostRestart), PostRestart)
  override def postStop(): Unit =
    next(behavior.management(ctx, PostStop), PostStop)
}
