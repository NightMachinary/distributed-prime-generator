
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSelection, Props}
import GeneralConstants._

import scala.concurrent.Await
import scala.concurrent.duration._


object DummyClient {
  def props(name: String): Props = Props(new DummyClient(name))

  final case object PukeClient

}

class DummyClient(name: String) extends Actor with ActorLogging {

  import DummyClient._
  import DummyProtocol._
  import context.dispatcher //Used for Scheduler implicit execution context.

  val masterSelection: ActorSelection =
    context.actorSelection(s"akka.tcp://$masterSystemName@$masterIP:$masterPort/user/$masterName")

  var master: Option[ActorRef] = None

  def resolveMaster(): Unit = {
    if (master.isDefined) {
      return
    }
    try {
      master = Some(Await.result(masterSelection.resolveOne(5.seconds), 5.seconds))
    }
    catch {
      case _: Throwable =>
        log.info("Could not resolve master. Trying again ...")
        resolveMaster()
    }

  }

  override def preStart(): Unit = {
    context.system.scheduler.schedule(2.seconds, 10.seconds, self, PukeClient)
  }

  override def receive: Receive =
    {
      case PukeClient =>
        log.info(s"$name is trying to greet master ...")
        masterSelection ! Hello(name)
    }


}
