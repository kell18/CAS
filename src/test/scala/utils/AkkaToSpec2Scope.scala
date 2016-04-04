package utils

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.specs2.mutable.After
import org.specs2.specification.Scope

/* A tiny class that can be used as a Specs2 'context'. */
abstract class AkkaToSpec2Scope(systemName: String = "TestingSystem", extraConfigs: String = "")
  extends TestKit(ActorSystem(systemName, ConfigFactory.parseString(extraConfigs)))
  with After
  with ImplicitSender
  with Scope {
  // make sure we shut down the actor system after all tests have run
  def after = system.shutdown()
}
