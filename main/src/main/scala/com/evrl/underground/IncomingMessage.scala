package com.evrl.underground

import com.lmax.disruptor.EventFactory

/**
 * Messages that are ran through the incoming disruptor. They can have an opcode,
 * which is "Message" by default, and some payload. A disruptor will pre-allocate
 * these to fill the initial ring buffer, which means that a messages "sits" in
 * the ring and gets filled with data as needed. That is why its fields are
 * mutable.
 */
class IncomingMessage(var data: Array[Byte], var operation: Operation.Opcode = Operation.Message) {

  // serialize this incoming message
  def asBytes: Array[Byte] = data

}

/**
 * Syntactic sugar mostly for testing - production code will not instantiate objects after
 * the ring buffer has been filled.
 */
object IncomingMessage {
  def apply(text: String) : IncomingMessage = new IncomingMessage(text.getBytes)
}

/**
 * A factory for the disruptor
 */
object IncomingMessageFactory extends EventFactory[IncomingMessage]{
  def newInstance = new IncomingMessage(null)
}

/**
 * The operation represented by the message.
 */
object Operation extends Enumeration {
  type Opcode = Value

  /**
   * A regular message
   */
  val Message = Value

  /**
   * "Please take a snapshot"
   */
  val Snapshot = Value

}
