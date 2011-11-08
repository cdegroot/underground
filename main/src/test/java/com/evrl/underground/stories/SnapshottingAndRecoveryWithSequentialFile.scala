package com.evrl.underground.stories

import java.util.UUID
import java.io.{ByteArrayInputStream, File}
import com.evrl.underground._
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach, FunSuite}
import testutils.IncomingMessageMatcherFactory._
import testutils.{SuiteOnBasicSequentialFilePersistence, JMockCycle}
import org.scalatest.Assertions._

class SnapshottingAndRecoveryWithSequentialFile extends SuiteOnBasicSequentialFilePersistence {
  import cycle._

  test("Snapshot operation will roll over log file and set data to snapshot base name") {

    persistence.persist(IncomingMessage("hello, "))
    persistence.persist(IncomingMessage("world"))
    val snapshotMessage = new IncomingMessage(null, Operation.Snapshot)
    persistence.persist(snapshotMessage)
    persistence.persist(IncomingMessage("bye"))

    assert(new File(logFileBase + "0").exists, "Old log file does not exist")
    assert(new File(logFileBase + "1").exists, "New log file was not created")

    var unMarshaller = new Unmarshaller(new ByteArrayInputStream(snapshotMessage.data))
    var fileName = unMarshaller.string
    var expectedFileName = persistence.baseDirName + "/snapshot.1"
    assert(expectedFileName.equals(fileName), "Did not find expected snapshot file name (saw %s, wanted %s)".format(fileName, expectedFileName))

    // and another snapshot
    persistence.persist(snapshotMessage)
    persistence.persist(IncomingMessage("again"))

    assert(new File(logFileBase + "0").exists, "Oldest log file was not preserved")
    assert(new File(logFileBase + "1").exists, "Middle log file was not preserved")
    assert(new File(logFileBase + "2").exists, "New log file was not created")

    unMarshaller = new Unmarshaller(new ByteArrayInputStream(snapshotMessage.data))
    fileName = unMarshaller.string
    expectedFileName = persistence.baseDirName + "/snapshot.2"
    assert(expectedFileName.equals(fileName), "Did not find expected snapshot file name (saw %s, wanted %s)".format(fileName, expectedFileName))

  }

  test("Recovery will pick up from correct point") {
    persistence.persist(IncomingMessage("hello, "))
    persistence.persist(IncomingMessage("world"))
    val snapshotMessage = new IncomingMessage(null, Operation.Snapshot)
    persistence.persist(snapshotMessage)
    new File(persistence.baseDirName + "/snapshot.1").createNewFile()
    persistence.persist(IncomingMessage("bye"))
    persistence.persist(snapshotMessage)
    new File(persistence.baseDirName + "/snapshot.2").createNewFile()
    persistence.persist(IncomingMessage("live again"))
    persistence.shutdown

    // We now have log 0, snapshot 1, log 1, snapshot 2, log 2.
    // recovery needs to be from snapshot 2 and then reply from log 2
    val recoverable = mock[Recoverable]
    expecting { e => import e._
      oneOf(recoverable).loadSnapshot(persistence.baseDirName + "/snapshot.2")
      oneOf(recoverable).processMessage(`with`(matchMessage("live again")))
    }
    whenExecuting {
      persistence.recoverTo(recoverable)
    }
  }

  test("Recovery will recover from just a 0 logfile") {
    persistence.persist(IncomingMessage("hello, "))
    persistence.persist(IncomingMessage("world"))

    val muncher = context.mock(classOf[Recoverable], "noShapshotRecoverable")
    expecting {
      e => import e._
      oneOf(muncher).processMessage(`with`(matchMessage("hello, ")))
      oneOf(muncher).processMessage(`with`(matchMessage("world")))
    }
    whenExecuting {
      persistence.recoverTo(muncher)
    }
  }

  test("After recovery, a new logfile has been created") {
    // TODO[CdG] a string constructor for IncomingMessage - it's getting silly
    persistence.persist(IncomingMessage("hello, "))
    persistence.shutdown

    persistence.recoverTo(new Recoverable {
      def processMessage(message: IncomingMessage) {}
      def loadSnapshot(snapshotFileName: String) {}
    })

    assert(persistence.sequenceNumber == 1, "sequence number should be 1, was %d".format(persistence.sequenceNumber))
    assert(persistence.logFile.exists(), "new log file does not exist")
  }
}
