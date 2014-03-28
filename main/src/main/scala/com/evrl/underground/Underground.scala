package com.evrl.underground

import com.lmax.disruptor.dsl.Disruptor
import java.util.concurrent.Executors
import com.lmax.disruptor.{EventHandler, WaitStrategy, ClaimStrategy}
import java.nio.ByteBuffer

/**
 * The core message processing class. Underground encapsulates a disruptor that is configured to
 * hand off message to replication and persistence first before sending it to the processing
 * logic. In this way, we know that messages are safe before we actually handle them.
 *
 * For purposes of testing and to allow for alternative configurations, all message handlers are optional.
 */
class Underground(replicator: Option[Replication],
                  persister: Option[Persistence],
                  processor: Option[ProcessingLogic],
                  ringBufferSize: Int = 128) extends IncomingDataHandler {

  val executor = Executors.newFixedThreadPool(3)

  val disruptor = new Disruptor(
    IncomingMessageFactory,
    ringBufferSize,
    executor,
    ClaimStrategy.Option.SINGLE_THREADED,
    WaitStrategy.Option.YIELDING)

  disruptor
    .handleEventsWith(ReplicationHandler(replicator), PersistenceHandler(persister))
    .then(ProcessingHandler(processor))

  val ringBuffer = disruptor.start

  override def process(message: Array[Byte]) {
    val sequence = ringBuffer.next
    val event = ringBuffer.get(sequence)
    event.data = message
    ringBuffer.publish(sequence)
  }

  def shutdown {
    disruptor.halt
  }
}

case class ReplicationHandler(replicator: Option[Replication]) extends EventHandler[IncomingMessage] {
  def onEvent(message: IncomingMessage, sequence: Long, endOfBatch: Boolean) {
    replicator.map(_.replicate(message))
  }
}

case class PersistenceHandler(persister: Option[Persistence]) extends EventHandler[IncomingMessage] {
  def onEvent(message: IncomingMessage, sequence: Long, endOfBatch: Boolean) {
    persister.map(_.persist(message))
  }
}

case class ProcessingHandler(processor: Option[ProcessingLogic]) extends EventHandler[IncomingMessage] {
  def onEvent(message: IncomingMessage,  sequence: Long,  endOfBatch: Boolean) {
    processor.map(_.process(message))
  }
}