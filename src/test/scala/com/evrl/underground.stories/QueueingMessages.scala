package com.evrl.underground

import com.evrl.underground.testutils.IncomingMessageMatcherFactory._
import org.scalatest.{BeforeAndAfterEach, FunSuite}
import org.junit.rules.Timeout
import org.scalatest.mock.JMockExpectations
import testutils.{IncomingMessageMatcher, JMockCycle}

/**
 * Story-level tests about queueing messages
 */
class QueueingMessages extends FunSuite with BeforeAndAfterEach {
  val cycle = new JMockCycle
  import cycle._

  val message = "Hello, world!"
  var underground : Underground = null;

  test("queued messages should be sent to persister") {
    val persister = mock[Persister]
    checkProcessing(None, Some(persister)) { (e, matcher) => import e._
      exactly(1).of(persister).persist(`with`(matcher))
    }
  }

  test("queued messages should be sent to replicator") {
    val replicator = mock[Replicator]
    checkProcessing(Some(replicator), None) { (e, matcher) => import e._
      exactly(1).of(replicator).replicate(`with`(matcher))
    }
  }

  override def afterEach {
    if (underground != null) {
      underground.shutdown
      underground = null
    }
  }

  def checkProcessing(replicator: Option[Replicator], persister: Option[Persister])
                     (expectations: (JMockExpectations, IncomingMessageMatcher) => Unit) {
    underground = new Underground(replicator, persister)
    val ready = context.states("ready").startsAs("no")
    expecting { e => import e._
      val matcher = matchMessage(message)
      expectations(e, matcher); then(ready.is("yes"))
    }
    whenExecuting  {
      underground.process(message.getBytes())
      while (!ready.is("yes").isActive) {
        Thread.`yield`
      }
    }
  }

}