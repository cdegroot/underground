package com.evrl.underground.stories

import org.scalatest.{BeforeAndAfterEach, FunSuite}
import com.evrl.underground.testutils.JMockCycle
import java.util.UUID
import java.io.{ByteArrayInputStream, File}
import com.evrl.underground._

class Snapshotting extends FunSuite with BeforeAndAfterEach {
  val cycle = new JMockCycle
  import cycle._

  val randomTestDir = System.getProperty("java.io.tmpdir") + "/ug-" + UUID.randomUUID
  val logFile = randomTestDir + "/message.log"


  override def beforeEach = new File(randomTestDir).mkdirs
  override def afterEach = {
    new File(logFile).delete
    new File(randomTestDir).delete
  }

  test("Snapshot operation will roll over log file and set data to snapshot base name") {
    val persister = new BasicSequentialFilePersistence(randomTestDir)

    persister.persist(new IncomingMessage("hello, ".getBytes))
    persister.persist(new IncomingMessage("world".getBytes))
    val snapshotMessage = new IncomingMessage(null, Operation.Snapshot)
    persister.persist(snapshotMessage)
    persister.persist(new IncomingMessage("bye".getBytes))
    persister.shutdown

    assert(new File(logFile).exists, "New log file was not created")
    assert(new File(randomTestDir + "/message.log.0").exists, "Old log file was not rotated")

    val unMarshaller = new Unmarshaller(new ByteArrayInputStream(snapshotMessage.data))
    val sequence = unMarshaller.int
    assert(sequence == 0, "Incorrect sequence number in data of operation (found " + sequence + ")")
  }
}