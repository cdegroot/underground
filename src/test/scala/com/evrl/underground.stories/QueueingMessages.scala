package com.evrl.underground

import com.evrl.underground.underground.Persister
import org.scalatest.FunSuite
import org.scalatest.mock.JMockCycle


/**
 * Story-level tests about queueing messages
 */
class QueueingMessages extends FunSuite {
  val cycle = new JMockCycle
  import cycle._

  val message = "Hello, world!"

  test("queued messages should be sent to persister") {
    val persister = mock[Persister]
    val underground = new Underground(None, Some(persister))

    expecting { e => import e._
      exactly(1).of(persister).persist(message)
    }
    whenExecuting {
      underground.send(null, message)
    }
  }

  test("queued messages should be sent to replicator") {
    val replicator = mock[Replicator]
    val underground = new Underground(Some(replicator), None)

    expecting { e => import e._
      exactly(1).of(replicator).replicate(message)
    }
    whenExecuting {
      underground.send(null, message)
    }
  }
}