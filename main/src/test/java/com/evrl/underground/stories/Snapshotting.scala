package com.evrl.underground.stories

import java.util.UUID
import java.io.{ByteArrayInputStream, File}
import com.evrl.underground._
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach, FunSuite}
import testutils.{SuiteOnBasicSequentialFilePersistence, JMockCycle}
import org.scalatest.Assertions._

class Snapshotting extends SuiteOnBasicSequentialFilePersistence {

  test("Snapshot operation will roll over log file and set data to snapshot base name") {

    persistence.persist(new IncomingMessage("hello, ".getBytes))
    persistence.persist(new IncomingMessage("world".getBytes))
    val snapshotMessage = new IncomingMessage(null, Operation.Snapshot)
    persistence.persist(snapshotMessage)
    persistence.persist(new IncomingMessage("bye".getBytes))

    assert(new File(logFileBase + "1").exists, "New log file was not created")
    assert(new File(logFileBase + "0").exists, "Old log file was not rotated")

    var unMarshaller = new Unmarshaller(new ByteArrayInputStream(snapshotMessage.data))
    var fileName = unMarshaller.string
    var expectedFileName = persistence.baseDirName + "/snapshot.1"
    assert(expectedFileName.equals(fileName), "Did not find expected snapshot file name (saw %s, wanted %s)".format(fileName, expectedFileName))

    // and another snapshot
    persistence.persist(snapshotMessage)
    persistence.persist(new IncomingMessage("again".getBytes))

    assert(new File(logFileBase + "0").exists, "Oldest log file was not preserved")
    assert(new File(logFileBase + "1").exists, "Middle log file was not preserved")
    assert(new File(logFileBase + "2").exists, "New log file was not created")

    unMarshaller = new Unmarshaller(new ByteArrayInputStream(snapshotMessage.data))
    fileName = unMarshaller.string
    expectedFileName = persistence.baseDirName + "/snapshot.2"
    assert(expectedFileName.equals(fileName), "Did not find expected snapshot file name (saw %s, wanted %s)".format(fileName, expectedFileName))

  }
}