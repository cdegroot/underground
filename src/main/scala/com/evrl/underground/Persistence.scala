package com.evrl.underground

/**
 * Responsible for message persistence (writing).
 */
trait Persistence {

  /**
   * Post-condition: the message has been persisted to a durable medium.
   */
  def persist(message: IncomingMessage)

  /**
   * Shutdown hook, can be used to close and release any resources.
   */
  def shutdown() {}

}

/**
 * Responsible for the recovery of messages from the the persist store (reading).
 */
trait MessageRecovery {

  /**
   * Loads all messages and presents them to the given sink.
   * @param sink processes recovered messages
   */
  def feedMessagesTo(sink: IncomingDataHandler)
}