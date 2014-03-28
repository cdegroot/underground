package com.evrl.underground.unit

import org.scalatest.FunSuite
import com.evrl.underground.{IncomingMessage, IncomingDataHandler, NetworkInput, SimpleNetworkReplicator}
import book.example.async.Timeout

/**
 * Basic unit test for our simple network replicator. We hook it up to
 * some listener and see whether it correctly forwards messages.
 */
class SimpleNetworkReplicatorTest extends FunSuite {

  test("basic replication") {
    @volatile var ticker : java.lang.Integer = 0
    val message = "Hello, world".getBytes
    val testHandler = new IncomingDataHandler {
      override def process(data: Array[Byte]): Unit = {
        assert(data.length == message.length + 4, "Unexpected data length, expected " + message.length + ", was " + data.length)
        ticker += 1
      }
    }
    val networkInput = new NetworkInput(testHandler, 0)
    val thread = new Thread(new Runnable {
      def run = networkInput.handleRequests
    })
    thread.start
    val replication = new SimpleNetworkReplicator(port = networkInput.serverPort)
    replication.replicate(new IncomingMessage(message))
    val timeout = new Timeout(500)
    while (ticker < 1 && !timeout.hasTimedOut) {
      Thread.`yield`
    }
    thread.stop
    assert(ticker == 1, "Expected one message to be seen, got " + ticker)
  }
}
