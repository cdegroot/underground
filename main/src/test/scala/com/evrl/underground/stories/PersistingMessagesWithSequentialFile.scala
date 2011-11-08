package com.evrl.underground.stories

import java.util.UUID
import org.junit.Assert.assertArrayEquals
import java.io.{FileOutputStream, FileInputStream, File}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach, FunSuite}
import com.evrl.underground.testutils.{SuiteOnBasicSequentialFilePersistence, JMockCycle}
import com.evrl.underground.testutils.IncomingMessageMatcherFactory._
import com.evrl.underground._

class PersistingMessagesWithSequentialFile extends SuiteOnBasicSequentialFilePersistence {
  import cycle._

  // I don't mock File here for two reasons: the class is a bitch to mock (as usual in
  // the JDK, whoever designed the thing should not be allowed to write a single line
  // of code again). Also, the class under test really is dependent on the file system,
  // most of the code in there should just pass on calls.

  test("SequentialFilePersistence persists data on reception") {
    val message = "Hello, world"
    val incomingMessage = IncomingMessage(message)

    persistence.persist(incomingMessage)
    persistence.shutdown

    val lf : File = new File(logFileBase + "0")
    assert(lf.exists, "log file was not created")
    val is = new FileInputStream(lf)
    val ms = new Unmarshaller(is)
    val messageLength = ms.int
    val buffer = new Array[Byte](messageLength)
    val numRead = is.read(buffer, 0, messageLength)
    is.close

    assert(numRead == message.length, "read back wrong number of characterstest")
    assertArrayEquals(buffer, message.getBytes)
  }

  test("SequentialFilePersistence will ignore partially written message") {
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
    val muncher = context.mock(classOf[Recoverable], "muncher2")
    expecting { e => import e._
      oneOf(muncher).processMessage(`with`(matchMessage(message1)))
    }
    whenExecuting {
      persistence.recoverTo(muncher)
    }
  }

  def logSomeMessagesTo(persister : SequentialFilePersistence) : (String, String) = {
    val message1 = "Hello"
    val message2 = ", world"
    val im1 = IncomingMessage(message1)
    val im2 = IncomingMessage(message2)

    persister.persist(im1)
    persister.persist(im2)

    return (message1, message2)
  }
}
