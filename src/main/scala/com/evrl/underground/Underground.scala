package com.evrl.underground

import com.lmax.disruptor.dsl.Disruptor
import java.util.concurrent.Executors
import com.lmax.disruptor.{EventHandler, WaitStrategy, ClaimStrategy}

class Underground(replicator: Option[Replicator],  persister: Option[Persister]) {

  val executor = Executors.newFixedThreadPool(3)

  // TODO - make at least the ring buffer size configurable
  val disruptor = new Disruptor(IncomingMessageFactory,
    100,
    executor,
    ClaimStrategy.Option.SINGLE_THREADED,
    WaitStrategy.Option.YIELDING)

  // TODO add queue processor
  disruptor.handleEventsWith(ReplicatorProcessor(replicator)).then(PersisterProcessor(persister))

  def send(queue: String, message: String) : Unit = {
    //disruptor.publishEvent
  }

}

case class ReplicatorProcessor(replicator: Option[Replicator]) extends EventHandler[IncomingMessage] {
  def onEvent(message: IncomingMessage, sequence: Long, endOfBatch: Boolean) {
    replicator.map(_.replicate(message))

  }
}

case class PersisterProcessor(persister: Option[Persister]) extends EventHandler[IncomingMessage] {
  def onEvent(message: IncomingMessage, sequence: Long, endOfBatch: Boolean) {
    persister.map(_.persist(message))
  }
}
