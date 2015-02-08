package actors

import akka.testkit.TestKit
import akka.testkit.ImplicitSender
import org.specs2.time.NoTimeConversions
import org.specs2.specification.AfterExample
import org.specs2.specification.script.SpecificationLike
import akka.actor.ActorSystem

abstract class AbstractTestKit(s: String) extends TestKit(ActorSystem(s))
  with AfterExample with ImplicitSender {
	
   override def after {
    system.shutdown
    system.awaitTermination
  }
}