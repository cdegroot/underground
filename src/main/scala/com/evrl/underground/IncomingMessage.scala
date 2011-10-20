package com.evrl.underground

import com.lmax.disruptor.EventFactory

sealed case class IncomingCommand;

// mutable state? Yup - that's one of the things that makes the disruptor so fast :)
class IncomingMessage {
  var incomingCommand : IncomingCommand = null;
  var queueName : String = null;
  var data : Array[Byte] = null;
}

object IncomingMessageFactory extends EventFactory[IncomingMessage]{
  def newInstance() = new IncomingMessage
}