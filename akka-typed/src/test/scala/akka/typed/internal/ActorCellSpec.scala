/**
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com/>
 */
package akka.typed
package internal

import org.scalactic.ConversionCheckedTripleEquals
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.exceptions.TestFailedException
import org.scalatest._
import org.junit.runner.RunWith

@RunWith(classOf[org.scalatest.junit.JUnitRunner])
class ActorCellSpec extends Spec with Matchers with BeforeAndAfterAll with ScalaFutures with ConversionCheckedTripleEquals {

  import ScalaDSL._

  val sys = new ActorSystemStub("ActorCellSpec")
  def ec = sys.controlledExecutor

  object `An ActorCell` {

    def `must be creatable`(): Unit = {
      val parent = new DebugRef[String](sys.path / "creatable", true)
      val cell = new ActorCell(sys, Props({ parent ! "created"; Static[String] { s ⇒ parent ! s } }), parent)
      debugCell(cell) {
        ec.queueSize should ===(0)
        cell.sendSystem(Create())
        ec.runOne()
        ec.queueSize should ===(0)
        parent.receiveAll() should ===(Right("created") :: Nil)
        cell.send("hello")
        ec.runOne()
        ec.queueSize should ===(0)
        parent.receiveAll() should ===(Right("hello") :: Nil)
      }
    }

    def `must be creatable with ???`(): Unit = {
      val parent = new DebugRef[String](sys.path / "creatable???", true)
      val self = new DebugRef[String](sys.path / "creatableSelf", true)
      val ??? = new NotImplementedError
      val cell = new ActorCell(sys, Props[String]({ parent ! "created"; throw ??? }), parent)
      cell.setSelf(self)
      debugCell(cell) {
        ec.queueSize should ===(0)
        cell.sendSystem(Create())
        ec.runOne()
        ec.queueSize should ===(0)
        parent.receiveAll() should ===(Right("created") :: Nil)
        self.receiveAll() should ===(Left(Terminate()) :: Nil)
        cell.sendSystem(Terminate())
        ec.runOne()
        ec.queueSize should ===(0)
        parent.receiveAll() should ===(Left(DeathWatchNotification(self, ???)) :: Nil)
      }
    }

    def `must be able to terminate after construction`(): Unit = {
      val parent = new DebugRef[String](sys.path / "terminate", true)
      val self = new DebugRef[String](sys.path / "terminateSelf", true)
      val cell = new ActorCell(sys, Props({ parent ! "created"; Stopped[String] }), parent)
      cell.setSelf(self)
      debugCell(cell) {
        ec.queueSize should ===(0)
        cell.sendSystem(Create())
        ec.runOne()
        ec.queueSize should ===(0)
        parent.receiveAll() should ===(Right("created") :: Nil)
        self.receiveAll() should ===(Left(Terminate()) :: Nil)
        cell.sendSystem(Terminate())
        ec.runOne()
        ec.queueSize should ===(0)
        parent.receiveAll() should ===(Left(DeathWatchNotification(self, null)) :: Nil)
      }
    }

    def `must be able to terminate after PreStart`(): Unit = {
      val parent = new DebugRef[String](sys.path / "terminate", true)
      val self = new DebugRef[String](sys.path / "terminateSelf", true)
      val cell = new ActorCell(sys, Props({ parent ! "created"; Full[String] { case Sig(ctx, PreStart) ⇒ Stopped } }), parent)
      cell.setSelf(self)
      debugCell(cell) {
        ec.queueSize should ===(0)
        cell.sendSystem(Create())
        ec.runOne()
        ec.queueSize should ===(0)
        parent.receiveAll() should ===(Right("created") :: Nil)
        self.receiveAll() should ===(Left(Terminate()) :: Nil)
        cell.sendSystem(Terminate())
        ec.runOne()
        ec.queueSize should ===(0)
        parent.receiveAll() should ===(Left(DeathWatchNotification(self, null)) :: Nil)
      }
    }

    def `must terminate upon failure during processing`(): Unit = {
      val parent = new DebugRef[String](sys.path / "terminate", true)
      val self = new DebugRef[String](sys.path / "terminateSelf", true)
      val ex = new AssertionError
      val cell = new ActorCell(sys, Props({ parent ! "created"; Static[String](s ⇒ throw ex) }), parent)
      cell.setSelf(self)
      debugCell(cell) {
        ec.queueSize should ===(0)
        cell.sendSystem(Create())
        ec.runOne()
        ec.queueSize should ===(0)
        parent.receiveAll() should ===(Right("created") :: Nil)
        cell.send("")
        ec.runOne()
        ec.queueSize should ===(0)
        self.receiveAll() should ===(Left(Terminate()) :: Nil)
        cell.sendSystem(Terminate())
        ec.runOne()
        ec.queueSize should ===(0)
        parent.receiveAll() should ===(Left(DeathWatchNotification(self, ex)) :: Nil)
      }
    }

    def `must signal failure when starting behavior is "same"`(): Unit = pending

    def `must signal failure when starting behavior is "unhandled"`(): Unit = pending

    def `must not reschedule for message when already activated`(): Unit = pending

    def `must not reschedule for signal when already activated`(): Unit = pending

    def `must reschedule for self-message`(): Unit = pending

    def `must reschedule when signal is enqueued after processing but before deactivation`(): Unit = pending

    def `must signal DeathWatch when terminating normally`(): Unit = pending

    def `must signal DeathWatch when terminating abnormally`(): Unit = pending

    def `must signal DeathWatch when watching after termination`(): Unit = pending

    def `must signal DeathWatch when watching after termination but before deactivation`(): Unit = pending

    def `must not signal DeathWatch after Unwatch has been processed`(): Unit = pending
  }

  private def debugCell[T, U](cell: ActorCell[T])(block: ⇒ U): U =
    try block
    catch {
      case ex: TestFailedException ⇒
        println(cell)
        throw ex
    }

}
