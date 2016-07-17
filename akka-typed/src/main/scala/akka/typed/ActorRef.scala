/**
 * Copyright (C) 2014-2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.typed

import akka.actor.ActorPath
import scala.annotation.unchecked.uncheckedVariance
import language.implicitConversions

/**
 * An ActorRef is the identity or address of an Actor instance. It is valid
 * only during the Actor’s lifetime and allows messages to be sent to that
 * Actor instance. Sending a message to an Actor that has terminated before
 * receiving the message will lead to that message being discarded; such
 * messages are delivered to the [[akka.actor.DeadLetter]] channel of the
 * [[akka.event.EventStream]] on a best effort basis
 * (i.e. this delivery is not reliable).
 */
abstract class ActorRef[-T](_path: ActorPath) extends java.lang.Comparable[ActorRef[Nothing]] { this: ScalaActorRef[T] with internal.ActorRefImpl[T] ⇒
  /**
   * Send a message to the Actor referenced by this ActorRef using *at-most-once*
   * messaging semantics.
   */
  def tell(msg: T): Unit

  /**
   * Unsafe utility method for widening the type accepted by this ActorRef;
   * provided to avoid having to use `asInstanceOf` on the full reference type,
   * which would unfortunately also work on non-ActorRefs.
   */
  def upcast[U >: T @uncheckedVariance]: ActorRef[U] = this.asInstanceOf[ActorRef[U]]

  /**
   * The hierarchical path name of the referenced Actor. The lifecycle of the
   * ActorRef is fully contained within the lifecycle of the [[akka.actor.ActorPath]]
   * and more than one Actor instance can exist with the same path at different
   * points in time, but not concurrently.
   */
  final val path: ActorPath = _path

  /**
   * Comparison takes path and the unique id of the actor cell into account.
   */
  final def compareTo(other: ActorRef[Nothing]) = {
    val x = this.path compareTo other.path
    if (x == 0) if (this.path.uid < other.path.uid) -1 else if (this.path.uid == other.path.uid) 0 else 1
    else x
  }

  final override def hashCode: Int = path.uid

  /**
   * Equals takes path and the unique id of the actor cell into account.
   */
  final override def equals(that: Any): Boolean = that match {
    case other: ActorRef[_] ⇒ path.uid == other.path.uid && path == other.path
    case _                  ⇒ false
  }

  final override def toString: String = s"Actor[${path}#${path.uid}]"
}

/**
 * This trait is used to hide the `!` method from Java code.
 */
trait ScalaActorRef[-T] { this: ActorRef[T] ⇒
  /**
   * Send a message to the Actor referenced by this ActorRef using *at-most-once*
   * messaging semantics.
   */
  def !(msg: T): Unit = tell(msg)
}

object ActorRef {
  //  private class Combined[T](val untypedRef: akka.actor.ActorRef) extends ActorRef[T](untypedRef.path) with ScalaActorRef[T] with internal.ActorRefImpl[T] {
  //    override def tell(msg: T): Unit = untypedRef.tell(msg, akka.actor.Actor.noSender)
  //  }

  implicit def toScalaActorRef[T](ref: ActorRef[T]): ScalaActorRef[T] = ref.asInstanceOf[ScalaActorRef[T]]

  /**
   * Construct a typed ActorRef from an untyped one and a protocol definition
   * (i.e. a recipient message type). This can be used to properly represent
   * untyped Actors within the typed world, given that they implement the assumed
   * protocol.
   */
  //  def apply[T](ref: akka.actor.ActorRef): ActorRef[T] = new Combined[T](ref)
}
