package com.evrl.underground.stories

import com.evrl.underground.testutils.IncomingMessageMatcherFactory._
import org.scalatest.{BeforeAndAfterEach, FunSuite}
import org.scalatest.mock.JMockExpectations
import com.evrl.underground.testutils.{IncomingMessageMatcher, JMockCycle}
import book.example.async.Timeout
import com.evrl.underground._

/**
 * Story-level tests about queueing messages
 */
class QueueingMessages extends FunSuite with BeforeAndAfterEach {
  val cycle = new JMockCycle
  import cycle._

  val message = "Hello, world!"
  var underground : Underground = null;

  test("queued messages should be sent to persister") {
    val persister = mock[Persistence]
    checkProcessing(None, Some(persister), None) { (e, matcher) => import e._
      exactly(1).of(persister).persist(`with`(matcher))
    }
  }

  test("queued messages should be sent to replicator") {
    val replicator = mock[Replication]
    checkProcessing(Some(replicator), None, None) { (e, matcher) => import e._
      exactly(1).of(replicator).replicate(`with`(matcher))
    }
  }

  test("queued messages should be sent to processor") {
    val processor = mock[ProcessingLogic]
    checkProcessing(None, None, Some(processor)) { (e, matcher) => import e._
      exactly(1).of(processor).process(`with`(matcher))}
  }

  test("queued messages should be replicated and persisted before being processed") {
    @volatile var ticker : java.lang.Integer = 0
    var persisterTick = 0
    var replicatorTick = 0
    var processorTick = 0
    val persister = new Persistence {
      def persist(message: IncomingMessage) = {
        ticker.synchronized {
          ticker += 1
          persisterTick = ticker
        }
      }
    }
    val replicator = new Replication {
      def replicate(message: IncomingMessage) = {
        ticker.synchronized {
          ticker += 1
          replicatorTick = ticker
        }
      }
    }
    val processor = new ProcessingLogic {
      def process(message: IncomingMessage) = {
        ticker.synchronized {
          ticker += 1
          processorTick = ticker
        }
      }
    }

    val timeout = new Timeout(500)
    underground = new Underground(Some(replicator), Some(persister), Some(processor))
    underground.process(message.getBytes())
    while (ticker < 3 && !timeout.hasTimedOut) {
      Thread.`yield`
    }
    assert(persisterTick > 0, "Persistence did not trigger")
    assert(replicatorTick > 0, "Replication did not trigger")
    assert(processorTick > 0, "ProcessingLogic did not trigger")
    assert(persisterTick < processorTick, "Persistence did not trigger before processor")
    assert(replicatorTick < processorTick, "Replication did not trigger before processor")
  }

  override def afterEach {
    if (underground != null) {
      underground.shutdown
      underground = null
    }
  }

  def checkProcessing(replicator: Option[Replication], persister: Option[Persistence], processor: Option[ProcessingLogic])
                     (expectations: (JMockExpectations, IncomingMessageMatcher) => Unit) {
    underground = new Underground(replicator, persister, processor)
    val ready = context.states("ready").startsAs("no")
    expecting { e => import e._
      val matcher = matchMessage(message)
      expectations(e, matcher); then(ready.is("yes"))
    }
    whenExecuting  {
      val timeout = new Timeout(500)
      underground.process(message.getBytes())
      while (!ready.is("yes").isActive && !timeout.hasTimedOut) {
        Thread.`yield`
      }
    }
  }

}