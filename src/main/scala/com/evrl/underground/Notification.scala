package com.evrl.underground

/**
 * Notify consumers of new messages.
 */
trait Notification {
  def notify(message: IncomingMessage)
}