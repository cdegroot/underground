package com.evrl.underground

/**
 * Interface definition for classes that persist messages in some way.
 */
trait Persister {
  def persist(message: IncomingMessage)

  def shutdown() {}

}

/**
 * The recovery part of something that persists
 */
trait MessageReplayer {
  def feedMessagesTo(sink : IncomingDataHandler)
}