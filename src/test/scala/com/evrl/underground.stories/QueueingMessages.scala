package com.evrl.underground

import com.evrl.underground.testutils.IncomingMessageMatcherFactory._
import org.scalatest.{BeforeAndAfterEach, FunSuite}
import testutils.JMockCycle
import org.junit.rules.Timeout

/**
 * Story-level tests about queueing messages
 */
class QueueingMessages extends FunSuite with BeforeAndAfterEach {
  val cycle = new JMockCycle
  import cycle._

  val message = "Hello, world!"

  var underground : Underground = null;

  override def afterEach {
    if (underground != null) {
      underground.shutdown
      underground = null
    }
  }

  test("queued messages should be sent to persister") {
    val persister = mock[Persister]
    underground = new Underground(None, Some(persister))
    val persisted = context.states("persisted").startsAs("nope")
    expecting { e => import e._
      val matcher = matchMessage(message)
      exactly(1).of(persister).persist(`with`(matcher)); then(persisted.is("yup"))
    }
    whenExecuting {
      underground.process(message.getBytes())
      while (!persisted.is("yup").isActive) {
        Thread.`yield`
      }
    }
  }

  test("queued messages should be sent to replicator") {
    val replicator = mock[Replicator]
    underground = new Underground(Some(replicator), None)

    expecting { e => import e._
      val matcher = matchMessage(message)
      exactly(1).of(replicator).replicate(`with`(matcher))
    }
    whenExecuting {
      underground.process(message.getBytes())
      Thread.sleep(1)
    }
  }
}