package com.evrl.underground

import java.io.{ByteArrayOutputStream, FileInputStream, FileOutputStream, File}
import java.util.UUID

/**
 * Very basic persistence that just writes everything to sequential log files.
 * Not thread safe, because meant to be called only from a single thread.
 * <br/>
 * Log files are created in the base directory as message.log.<sequence number>. The
 * sequence number increases every time a snapshot message is received. For example, if
 * the directory has:
 * <pre>
 * message.log.00000000
 * message.log.00000001
 * message.log.00000002
 * </pre>
 * then snapshots 1 and 2 are expected to exist in the same directory:
 * <pre>
 * snapshot.00000001
 * snapshot.00000002
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

  // TODO[cdg] - how to make this immutable? Do we need/want to?
  var logFile = new File(base, "message.log")
  var writeStream = new FileOutputStream(logFile)
  var marshall = new Marshaller(writeStream)

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

  def snapshot(message : IncomingMessage) {
    shutdown
    logFile.renameTo(new File(logFile.getAbsolutePath + ".0"))
    writeStream = new FileOutputStream(logFile)
    marshall = new Marshaller(writeStream)
    writeSequenceToData(0, message)
  }

  override def shutdown {
    writeStream.close
  }

  def writeSequenceToData(seq : Int, message : IncomingMessage) {
    val bos = new ByteArrayOutputStream(4)
    val dataMarshall = new Marshaller(bos)
    dataMarshall.int(seq)
    message.data = bos.toByteArray
  }

  def feedMessagesTo(sink : IncomingDataHandler) {
    val readStream = new FileInputStream(logFile)
    val unmarshalled = new Unmarshaller(readStream)
    val totalLength = logFile.length()
    var processedLength = 0
    while (processedLength < totalLength) {
      val messageLength = unmarshalled.int
      val data = new Array[Byte](messageLength)
      if (messageLength > 0) {
        val numRead = readStream.read(data, 0, messageLength)
        if (numRead == messageLength) {
          sink.process(data)
        }
      }
      processedLength = processedLength + messageLength + 4
    }
    readStream.close()
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
    val randomDir = System.getProperty("java.io.tmpdir") + "/ug-" + UUID.randomUUID
    new SequentialFilePersistence(randomDir)
  }
}