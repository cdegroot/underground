package com.evrl.underground

import java.io.IOException
import java.lang.Thread
import java.nio.ByteBuffer
import java.nio.channels.spi.SelectorProvider
import java.nio.channels.{SocketChannel, ServerSocketChannel, SelectionKey, Selector}
import java.util.Arrays
import java.net.{InetSocketAddress, Socket, ServerSocket}

/**
 * This class glues the disruptor to the outside world.
 */
class NetworkInput(sink: IncomingDataHandler, val listeningPort: Int = 0xcafe) {

  val serverSocketChannel = ServerSocketChannel.open()
  serverSocketChannel.configureBlocking(false)
  serverSocketChannel.socket.bind(new InetSocketAddress(listeningPort))

  val selector = SelectorProvider.provider.openSelector
  serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT)

  val readBufferSize = 32 * 1024
  val readBuffer = ByteBuffer.allocate(readBufferSize)

  /** Just in case you pass in 0 for the listingPort, you might want to
    * know where we end up listening.
    * @return The listening port.
    */
  lazy val serverPort = serverSocketChannel.socket().getLocalPort

  private def accept(key: SelectionKey) {
    val serverSocketChannel = key.channel.asInstanceOf[ServerSocketChannel]
    val socketChannel = serverSocketChannel.accept
    socketChannel.configureBlocking(false)
    socketChannel.register(selector, SelectionKey.OP_READ)
  }

  private def read(key: SelectionKey) {
    val socketChannel = key.channel.asInstanceOf[SocketChannel]
    readBuffer.clear
    var numRead = -1
    try {
      numRead = socketChannel.read(this.readBuffer)
    } catch {
      case e: IOException =>
        key.cancel
        socketChannel.close
        return
    }

    if (numRead == -1) {
      key.cancel
      socketChannel.close
      return
    }

    val bytes = Arrays.copyOf(readBuffer.array(), numRead)
    sink.process(bytes)
  }

  def handleRequests {
    try {
      while (true) {
        selector.select
        val selectedKeys = selector.selectedKeys.iterator
        while (selectedKeys.hasNext) {
          val key = selectedKeys.next()
          selectedKeys.remove()

          if (key.isValid()) {

            if (key.isAcceptable()) {
              accept(key)
            } else if (key.isReadable()) {
              read(key)
            }
          }
        }
      }
    } catch {
      case e: IOException =>
        System.out.println(e)
    }
  }
}