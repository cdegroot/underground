package com.evrl.underground

import com.evrl.underground.testutils.IncomingMessageMatcherFactory._
import org.scalatest.{BeforeAndAfterEach, FunSuite}
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
    checkProcessing(None, Some(persister), None) { (e, matcher) => import e._
      exactly(1).of(persister).persist(`with`(matcher))
    }
  }

  test("queued messages should be sent to replicator") {
    val replicator = mock[Replicator]
    checkProcessing(Some(replicator), None, None) { (e, matcher) => import e._
      exactly(1).of(replicator).replicate(`with`(matcher))
    }
  }

  test("queued messages should be sent to processor") {
    val processor = mock[Processor]
    checkProcessing(None, None, Some(processor)) { (e, matcher) => import e._
      exactly(1).of(processor).process(`with`(matcher))}
  }

  test("queued messages should be replicated and persisted before being processed") {
    @volatile var ticker : java.lang.Integer = 0
    var persisterTick = 0
    var replicatorTick = 0
    var processorTick = 0
    val persister = new Persister {
      def persist(message: IncomingMessage) = {
        ticker.synchronized {
          ticker += 1
          persisterTick = ticker
        }
      }
    }
    val replicator = new Replicator {
      def replicate(message: IncomingMessage) = {
        ticker.synchronized {
          ticker += 1
          replicatorTick = ticker
        }
      }
    }
    val processor = new Processor {
      def process(message: IncomingMessage) = {
        ticker.synchronized {
          ticker += 1
          processorTick = ticker
        }
      }
    }

    underground = new Underground(Some(replicator), Some(persister), Some(processor))
    underground.process(message.getBytes())
    while (ticker < 3) {
      Thread.`yield`
    }
    assert(persisterTick > 0)
    assert(replicatorTick > 0)
    assert(processorTick > 0)
    assert(persisterTick < processorTick)
    assert(replicatorTick < processorTick)
  }

  override def afterEach {
    if (underground != null) {
      underground.shutdown
      underground = null
    }
  }

  def checkProcessing(replicator: Option[Replicator], persister: Option[Persister], processor: Option[Processor])
                     (expectations: (JMockExpectations, IncomingMessageMatcher) => Unit) {
    underground = new Underground(replicator, persister, processor)
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