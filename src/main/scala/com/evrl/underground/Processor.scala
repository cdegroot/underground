package com.evrl.underground

trait Processor {
  def process(message: IncomingMessage)
}