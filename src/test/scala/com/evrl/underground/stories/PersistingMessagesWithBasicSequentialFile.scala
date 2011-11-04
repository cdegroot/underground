package com.evrl.underground.stories

import java.util.UUID
import org.junit.Assert.assertArrayEquals
import com.evrl.underground.{Unmarshaller, IncomingDataHandler, IncomingMessage, BasicSequentialFilePersistence}
import java.io.{FileOutputStream, FileInputStream, File}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach, FunSuite}
import com.evrl.underground.testutils.{SuiteOnBasicSequentialFilePersistence, JMockCycle}

class PersistingMessagesWithBasicSequentialFile extends SuiteOnBasicSequentialFilePersistence {
  import cycle._

  // I don't mock File here for two reasons: the class is a bitch to mock (as usual in
  // the JDK, whoever designed the thing should not be allowed to write a single line
  // of code again). Also, the class under test really is dependent on the file system,
  // most of the code in there should just pass on calls.

  test("MarshallerTest persists data on reception") {
    val message = "Hello, world".getBytes
    val incomingMessage = new IncomingMessage(message)

    persistence.persist(incomingMessage)
    persistence.shutdown

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
    val (message1, message2) = logSomeMessagesTo(persistence)
    persistence.shutdown()

    val muncher = mock[IncomingDataHandler]
    expecting { e => import e._
      oneOf(muncher).process(message1)
      oneOf(muncher).process(message2)
    }
    whenExecuting {
      persistence.feedMessagesTo(muncher)
    }
  }

  test("Marshaller will ignore partially written message") {
    val (message1, message2) = logSomeMessagesTo(persistence)
    persistence.shutdown()

    // Now copy, truncate, and write short output
    val readStream = new FileInputStream(persistence.logFile)
    val buffer = new Array[Byte](1024)
    val oldLength = readStream.read(buffer)
    readStream.close()
    persistence.logFile.delete()
    val writeStream = new FileOutputStream(persistence.logFile)
    writeStream.write(buffer, 0, oldLength - 2)
    writeStream.close()

    // garbled message should be silently dropped
    val muncher = context.mock(classOf[IncomingDataHandler], "muncher2")
    expecting { e => import e._
      oneOf(muncher).process(message1)
    }
    whenExecuting {
      persistence.feedMessagesTo(muncher)
    }
  }

  def logSomeMessagesTo(persister : BasicSequentialFilePersistence) : (Array[Byte], Array[Byte]) = {
    val message1 = "Hello".getBytes
    val message2 = ", world".getBytes
    val im1 = new IncomingMessage(message1)
    val im2 = new IncomingMessage(message2)

    persister.persist(im1)
    persister.persist(im2)

    return (message1, message2)
  }
}