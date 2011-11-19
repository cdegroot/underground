package com.evrl.underground

import java.nio.ByteBuffer

/**
 * Describes something that can munch incoming data from a wire or similar.
 */
trait IncomingDataHandler {
  def process(data : Array[Byte])

}