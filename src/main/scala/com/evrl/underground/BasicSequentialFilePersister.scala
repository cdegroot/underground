package com.evrl.underground

import java.io.{FileInputStream, FileOutputStream, File}
import com.sun.jmx.remote.internal.Unmarshal
import java.rmi.MarshalledObject

/**
 * A very basic persister that just writes everything to a sequential log file. Not thread safe,
 * meant to be called only from a single thread
 */
class BasicSequentialFilePersister(baseDirName: String) extends Persister with MessageReplayer {

  val base = new File(baseDirName)
  base.mkdirs()

  val logFile = new File(base, "message.log")
  val writeStream = new FileOutputStream(logFile)
  val marshall = new Marshaller(writeStream)

  override def persist(message : IncomingMessage) {
    val bytes = message.asBytes
    marshall.int(bytes.length)
    writeStream.write(bytes)
  }

  override def shutdown {
    writeStream.close
  }

  override def feedMessagesTo(sink : IncomingDataHandler) {
    val readStream = new FileInputStream(logFile)
    val unmarshall = new Unmarshaller(readStream)
    val totalLength = logFile.length()
    var processedLength = 0
    while (processedLength < totalLength) {
      val messageLength = unmarshall.int
      val data = new Array[Byte](messageLength)
      if (messageLength > 0) {
        readStream.read(data, 0, messageLength)
        sink.process(data)
      }
      processedLength = processedLength + messageLength + 4
    }
    readStream.close
  }
}