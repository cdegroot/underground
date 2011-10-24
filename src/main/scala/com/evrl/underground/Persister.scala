package com.evrl.underground

trait Persister {
  def persist(message: IncomingMessage)
}