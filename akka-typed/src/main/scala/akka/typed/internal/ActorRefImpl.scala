/**
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com/>
 */
package akka.typed
package internal

import akka.{ actor ⇒ a }
import akka.dispatch.sysmsg._

private[typed] trait ActorRefImpl[-T] extends ActorRef[T] { this: ScalaActorRef[T] ⇒
  def sendSystem(signal: SystemMessage): Unit
  def isLocal: Boolean
}

private[typed] class LocalActorRef[-T](_path: a.ActorPath, cell: ActorCell[T])
  extends ActorRef[T](_path) with ActorRefImpl[T] with ScalaActorRef[T] {
  override def tell(msg: T): Unit = cell.send(msg)
  override def sendSystem(signal: SystemMessage): Unit = cell.sendSystem(signal)
  final override def isLocal: Boolean = true
}

private[typed] final class FunctionRef[-T](_path: a.ActorPath, override val isLocal: Boolean,
                                           send: T ⇒ Unit)
  extends ActorRef[T](_path) with ActorRefImpl[T] with ScalaActorRef[T] {
  override def tell(msg: T): Unit = send(msg)
  override def sendSystem(signal: SystemMessage): Unit = signal match {
    case Create()                           ⇒ // nothing to do
    case DeathWatchNotification(ref, cause) ⇒ // FIXME
    case Terminate()                        ⇒ // FIXME
    case Watch(watchee, watcher)            ⇒ // FIXME
    case Unwatch(watchee, watcher)          ⇒ // FIXME
    case NoMessage                          ⇒ // nothing to do
  }
}
