/**
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com/>
 */
package akka.typed
package internal

import org.junit.runner.RunWith
import org.scalactic.ConversionCheckedTripleEquals
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.concurrent.Eventually
import scala.concurrent.duration._
import scala.concurrent.{ Future, Promise }

@RunWith(classOf[org.scalatest.junit.JUnitRunner])
class ActorSystemSpec extends Spec with Matchers with BeforeAndAfterAll with ScalaFutures with Eventually with ConversionCheckedTripleEquals {
  import ScalaDSL._

  override implicit val patienceConfig = PatienceConfig(1.second)

  case class Probe(msg: String, replyTo: ActorRef[String])

  trait CommonTests {
    def system[T](name: String, props: Props[T]): ActorSystem[T]

    def `must start the guardian actor and terminate when it terminates`(): Unit = {
      val sys = system("a", Props(Total[Probe] { p ⇒ p.replyTo ! p.msg; Stopped }))
      val inbox = Inbox[String]("a")
      sys ! Probe("hello", inbox.ref)
      eventually { inbox.hasMessages should ===(true) }
      inbox.receiveAll() should ===("hello" :: Nil)
      val p = sys.whenTerminated.futureValue.ref.path
      p.name should ===("/")
      p.address.system should ===("a")
    }

    def `must terminate the guardian actor`(): Unit = {
      val inbox = Inbox[String]("terminate")
      val sys = system("terminate", Props(Full[Probe] {
        case Sig(ctx, PostStop) ⇒
          inbox.ref ! "done"
          Same
      }))
      sys.terminate().futureValue
      inbox.receiveAll() should ===("done" :: Nil)
    }

    def `must log to the event stream`(): Unit = pending

    def `must have a name`(): Unit = {
      val sys = system("name", Props(Empty))
      sys.name should ===("name")
      sys.terminate().futureValue
    }

    def `must report its uptime`(): Unit = {
      val sys = system("uptime", Props(Empty))
      val up = sys.uptime
      up should be < 500L
      Thread.sleep(300)
      sys.uptime should be >= (up + 300)
      sys.terminate().futureValue
    }

    def `must have a working thread factory`(): Unit = {
      val sys = system("thread", Props(Empty))
      val p = Promise[Int]
      sys.threadFactory.newThread(new Runnable {
        def run(): Unit = p.success(42)
      }).start()
      p.future.futureValue should ===(42)
      sys.terminate().futureValue
    }

    def `must be able to run Futures`(): Unit = {
      val sys = system("thread", Props(Empty))
      val f = Future(42)(sys.executionContext)
      f.futureValue should ===(42)
      sys.terminate().futureValue
    }

    // this is essential to complete ActorCellSpec, see there
    def `must correctly treat Watch dead letters`(): Unit = {
      val sys = system("deadletters", Props(Empty))
      val client = new DebugRef[Int](sys.path / "debug", true)
      sys.deadLetters.toImpl.sendSystem(Watch(sys, client))
      client.receiveAll() should ===(Left(DeathWatchNotification(sys, null)) :: Nil)
      sys.terminate().futureValue
    }
  }

  object `An ActorSystemImpl` extends CommonTests {
    def system[T](name: String, props: Props[T]): ActorSystem[T] = ActorSystem(name, props)
  }

  object `An ActorSystemAdapter` extends CommonTests {
    def system[T](name: String, props: Props[T]): ActorSystem[T] = ActorSystem(name, props)
  }
}
