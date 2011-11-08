package com.evrl.underground

import java.util.UUID
import java.io._

/**
 * Very basic persistence that just writes everything to sequential log files.
 * Not thread safe, because meant to be called only from a single thread.
 * <br/>
 * Log files are created in the base directory as message.log.<sequence number>. The
 * sequence number increases every time a snapshot message is received. For example, if
 * the directory has:
 * <pre>
 * message.log.0
 * message.log.1
 * message.log.2
 * </pre>
 * then snapshots 1 and 2 are expected to exist in the same directory:
 * <pre>
 * snapshot.1
 * snapshot.2
 * </pre>
 * And on recovery, snapshot 2 will be loaded, after which message log 2 will be
 * loaded and fed to the processor.
 * <br/>
 * This class is not responsible for snapshot writing and reading - the processing
 * logic needs to do that. However, on a snapshot command, the class will put the
 * expected file name of the snapshot file into the message, and on recovery, the
 * class will work with the processing logic to load the correct snapshot and
 * replay the correct messages (through the SnapshotRecovery trait).
 */
class SequentialFilePersistence(val baseDirName: String) extends Persistence with MessageRecovery {

  val base = new File(baseDirName)
  base.mkdirs()

  var logFileTemplate = "message.log."
  var snapshotTemplate = "snapshot."
  var sequenceNumber = 0
  var logFile: File = _
  var writeStream: OutputStream = _
  var marshall: Marshaller = _
  openLogFile

  override def persist(message : IncomingMessage) {
    message.operation match {
      case Operation.Message => logMessage(message)
      case Operation.Snapshot => snapshot(message)
    }
  }

  def logMessage(message : IncomingMessage) {
    val bytes = message.asBytes
    marshall.int(bytes.length)
    writeStream.write(bytes)
  }


  def openLogFile {
    logFile = new File(base, logFileTemplate + sequenceNumber)
    writeStream = new FileOutputStream(logFile)
    marshall = new Marshaller(writeStream)
  }

  def snapshot(message : IncomingMessage) {
    shutdown
    sequenceNumber = sequenceNumber + 1
    writeSnapshotNameToMessage(sequenceNumber, message)
    openLogFile
  }

  override def shutdown {
    writeStream.close
  }

  private def writeSnapshotNameToMessage(seq : Int, message : IncomingMessage) {
    val bos = new ByteArrayOutputStream(60)
    val dataMarshall = new Marshaller(bos)
    dataMarshall.string(new File(base, snapshotTemplate + seq).getAbsolutePath)
    message.data = bos.toByteArray
  }

  def sequenceNumberOf(file: File) : Int = {
    val name = file.getName
    name.substring(name.lastIndexOf('.') + 1).toInt
  }

  private def findLatestSnapshotSequenceNumber : Option[Int] = {
    val files = base.listFiles()
    val snapshots = files.
      filter(_.getName.startsWith(snapshotTemplate)).
      sortWith((f1, f2) => {sequenceNumberOf(f1) > sequenceNumberOf(f2)})
    if (snapshots.length == 0) {
      None
    } else {
      Some(sequenceNumberOf(snapshots.head))
    }
  }

  override def recoverTo(recoverable : Recoverable) {
    val latestSnapshotSequenceNumber = findLatestSnapshotSequenceNumber
    latestSnapshotSequenceNumber.map{seqNo =>
      val snapshotToRecoverFrom = snapshotTemplate + seqNo
      recoverable.loadSnapshot(new File(base,snapshotToRecoverFrom).getAbsolutePath)
    }
    val logFileToRecoverFrom = logFileTemplate + latestSnapshotSequenceNumber.getOrElse(0)
    val readStream = new FileInputStream(new File(base, logFileToRecoverFrom))
    val unmarshalled = new Unmarshaller(readStream)
    val totalLength = logFile.length()
    var processedLength = 0
    while (processedLength < totalLength) {
      val messageLength = unmarshalled.int
      val data = new Array[Byte](messageLength)
      if (messageLength > 0) {
        val numRead = readStream.read(data, 0, messageLength)
        if (numRead == messageLength) {
          recoverable.processMessage(new IncomingMessage(data))
        }
      }
      processedLength = processedLength + messageLength + 4
    }
    readStream.close()
    shutdown
    sequenceNumber = latestSnapshotSequenceNumber.getOrElse(0) + 1
    openLogFile
  }

  /**
   * Dangerous! Destroy all data (up and including baseDir)
   */
  def destroyData {
    base.listFiles().map(_.delete())
    base.delete()
  }
}

object SequentialFilePersistence {
  def onRandomDirectory : SequentialFilePersistence = {
    val randomDir = new File(new File(System.getProperty("java.io.tmpdir")), "ug-" + UUID.randomUUID).getAbsolutePath
    new SequentialFilePersistence(randomDir)
  }
}