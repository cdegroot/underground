package com.evrl.underground.stories

import org.scalatest.{BeforeAndAfterEach, FunSuite}
import com.evrl.underground.testutils.JMockCycle
import java.util.UUID
import java.io.{FileInputStream, File}
import org.junit.Assert.assertArrayEquals
import com.evrl.underground.{Unmarshaller, IncomingDataHandler, IncomingMessage, BasicSequentialFilePersister}

class PersistingMessagesWithBasicSequentialFile extends FunSuite with BeforeAndAfterEach {
  val cycle = new JMockCycle
  import cycle._

  val randomTestDir = System.getProperty("java.io.tmpdir") + "/ug-" + UUID.randomUUID
  val logFile = randomTestDir + "/message.log"

  override def beforeEach = new File(randomTestDir).mkdirs
  override def afterEach = {
    new File(logFile).delete
    new File(randomTestDir).delete
  }

  // I don't mock File here for two reasons: the class is a bitch to mock (as usual in
  // the JDK, whoever designed the thing should not be allowed to write a single line
  // of code again). Also, the class under test really is dependent on the file system,
  // most of the code in there should just pass on calls.
  test("MarshallerTest persists data on reception") {
    // One of these brittle tests. However, we'll need to ensure that stuff
    // actually lands on disk...
    val persister = new BasicSequentialFilePersister(randomTestDir)
    val message = "Hello, world".getBytes
    val incomingMessage = new IncomingMessage(message)

    persister.persist(incomingMessage)
    persister.shutdown

    val lf : File = new File(logFile)
    assert(lf.exists, "log file was not created")
    val is = new FileInputStream(lf)
    val ms = new Unmarshaller(is)
    val messageLength = ms.int
    val buffer = new Array[Byte](messageLength)
    val numRead = is.read(buffer, 0, messageLength)
    is.close

    assert(numRead == message.length, "read back wrong number of characterstest")
    assertArrayEquals(buffer, message)
  }

  test("MarshallerTest persists multiple messages and can feed them back") {
    val persister = new BasicSequentialFilePersister(randomTestDir)
    val message1 = "Hello".getBytes
    val message2 = ", world".getBytes
    val im1 = new IncomingMessage(message1)
    val im2 = new IncomingMessage(message2)

    persister.persist(im1)
    persister.persist(im2)
    persister.shutdown()

    val muncher = mock[IncomingDataHandler]
    expecting { e => import e._
      oneOf(muncher).process(message1)
      oneOf(muncher).process(message2)
    }
    whenExecuting {
      persister.feedMessagesTo(muncher)
    }
  }
}