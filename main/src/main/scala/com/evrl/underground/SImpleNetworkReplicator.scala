package com.evrl.underground

import java.nio.channels.SocketChannel
import java.net.{SocketAddress, Socket}
import java.nio.ByteBuffer

/**
 * A very basic replication class that gets handed a network
 * channel to replicate to. Replication is deemed a success
 * when the TCP tells it is ok. This is a completely silly
 * implementation, it doesn't do any streaming, sliding
 * windows, smart use of the disruptor's batch flag,
 * advanced error checking, and whatnot. However,
 * make it work still comes first ;-)
 */
class SimpleNetworkReplicator(host: String = "::1", port: Int = 0xca5e) extends Replication {

  // We want to synchronously wait in this implementation so no NIO fanciness
  val clientSocket = new Socket(host, port)
  val clientStream = clientSocket.getOutputStream
  val marshaller = new Marshaller(clientStream)

  /**
   * Post condition: message has been persisted on another broker.
   */
  override def replicate(message: IncomingMessage): Unit = {
    // TODO this message business needs cleaning, especially around snapshot messages. For now, we just replicate data
    marshaller.int(message.data.length)
    clientStream.write(message.data)
    clientStream.flush()
  }
}
