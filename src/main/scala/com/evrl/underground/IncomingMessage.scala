package com.evrl.underground

import com.lmax.disruptor.EventFactory

// mutable state? Yup - that's one of the things that makes the disruptor so fast :)
// as for now we are not interested in the contents of messages until they leave the
// ring, we just shove in the bytes from the wire, to be interpreted later. If
// we want, later on, message parsing can be part of the ring buffer's processing
// pipeline (will put another core to good use :-))
class IncomingMessage(var data : Array[Byte]) {

  // serialize this incoming message
  def asBytes: Array[Byte] = data
}

object IncomingMessageFactory extends EventFactory[IncomingMessage]{
  def newInstance = new IncomingMessage(null)
}