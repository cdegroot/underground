package com.evrl.underground.stories

import org.scalatest.FunSuite
import com.evrl.underground.testutils.JMockCycle
import com.evrl.underground.{NetworkInput, IncomingDataHandler}
import java.net.Socket
import java.lang.Thread

class ReadingFromNetwork extends FunSuite {
  val cycle = new JMockCycle
  import cycle._

  test("Reading from network results in packets landing at incoming data handler") {
    val mockHandler = mock[IncomingDataHandler]
    val networkInput = new NetworkInput(mockHandler)
    val message = "Hello, world".getBytes
    expecting { e => import e._
      oneOf(mockHandler).process(`with`(message))
    }
    whenExecuting {
      val thread = new Thread(new Runnable {
        def run = networkInput.handleRequests
      })
      thread.start
      val socket = new Socket("localhost", networkInput.listeningPort)
      socket.getOutputStream.write(message)
      Thread.sleep(1000) // TODO[CdG] fixme - we don't want sleeps in tests
      socket.close
      thread.stop
    }
  }

}