package com.evrl.underground.stories

import java.util.UUID
import java.io.{ByteArrayInputStream, File}
import com.evrl.underground._
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach, FunSuite}
import testutils.{SuiteOnBasicSequentialFilePersistence, JMockCycle}

class Snapshotting extends SuiteOnBasicSequentialFilePersistence {

  test("Snapshot operation will roll over log file and set data to snapshot base name") {

    persistence.persist(new IncomingMessage("hello, ".getBytes))
    persistence.persist(new IncomingMessage("world".getBytes))
    val snapshotMessage = new IncomingMessage(null, Operation.Snapshot)
    persistence.persist(snapshotMessage)
    persistence.persist(new IncomingMessage("bye".getBytes))
    persistence.shutdown

    assert(new File(logFile).exists, "New log file was not created")
    assert(new File(persistence.baseDirName + "/message.log.0").exists, "Old log file was not rotated")

    val unMarshaller = new Unmarshaller(new ByteArrayInputStream(snapshotMessage.data))
    val sequence = unMarshaller.int
    assert(sequence == 0, "Incorrect sequence number in data of operation (found " + sequence + ")")
  }
}