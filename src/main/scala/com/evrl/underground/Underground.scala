package com.evrl.underground

import com.lmax.disruptor.dsl.Disruptor
import java.util.concurrent.Executors
import com.lmax.disruptor.{EventHandler, WaitStrategy, ClaimStrategy}

class Underground(replicator: Option[Replicator],  persister: Option[Persister], processor: Option[Processor]) {

  val executor = Executors.newFixedThreadPool(3)

  // TODO - make at least the ring buffer size configurable
  val disruptor = new Disruptor(
    IncomingMessageFactory,
    128,
    executor,
    ClaimStrategy.Option.SINGLE_THREADED,
    WaitStrategy.Option.YIELDING)

  disruptor
    .handleEventsWith(ReplicationHandler(replicator), PersistenceHandler(persister))
    .then(ProcessingHandler(processor))

  val ringBuffer = disruptor.start

  def process(message: Array[Byte]) {
    val sequence = ringBuffer.next
    val event = ringBuffer.get(sequence)
    event.data = message
    ringBuffer.publish(sequence)
  }

  def shutdown {
    disruptor.halt
  }
}

case class ReplicationHandler(replicator: Option[Replicator]) extends EventHandler[IncomingMessage] {
  def onEvent(message: IncomingMessage, sequence: Long, endOfBatch: Boolean) {
    replicator.map(_.replicate(message))
  }
}

case class PersistenceHandler(persister: Option[Persister]) extends EventHandler[IncomingMessage] {
  def onEvent(message: IncomingMessage, sequence: Long, endOfBatch: Boolean) {
    persister.map(_.persist(message))
  }
}

case class ProcessingHandler(processor: Option[Processor]) extends EventHandler[IncomingMessage] {
  def onEvent(message: IncomingMessage,  sequence: Long,  endOfBatch: Boolean) {
    processor.map(_.process(message))
  }
}