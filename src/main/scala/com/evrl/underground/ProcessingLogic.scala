package com.evrl.underground

/**
 * Actually process the message - parse, interpret, ...
 */
trait ProcessingLogic {
  def process(message: IncomingMessage)
}