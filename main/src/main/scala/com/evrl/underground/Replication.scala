package com.evrl.underground

/**
 * Arranges for replication to a peer broker for increased availability
 * in case of failure of this broker.
 */
trait Replication {

  /**
   * Post condition: message has been persisted on another broker.
   */
  def replicate(message: IncomingMessage)

}