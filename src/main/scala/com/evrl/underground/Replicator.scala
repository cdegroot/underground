package com.evrl.underground

trait Replicator {
  def replicate(message: IncomingMessage)
}