package com.evrl.underground

/**
 * Describes something that can munch incoming data from a wire or similar.
 */
trait IncomingDataHandler {
  def process(data : Array[Byte])
}