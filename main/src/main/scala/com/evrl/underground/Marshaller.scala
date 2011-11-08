package com.evrl.underground

import java.io.{OutputStream, InputStream}
import java.nio.charset.Charset
import java.lang.String

/**
 * A very straightforward marshaller. This marshaller is, as you can see, decidedly not multi-threaded. This
 * is a design feature :)
 */
class Marshaller(val oos : OutputStream) {
  val buf = new Array[Byte](4)

  def int(value: Int) {
    buf(0) = (value >> 24).asInstanceOf[Byte];
    buf(1) = (value >> 16).asInstanceOf[Byte];
    buf(2) = (value >> 8).asInstanceOf[Byte];
    buf(3) = value.asInstanceOf[Byte]
    oos.write(buf, 0, 4)
  }

  def string(s: String) {
    val bytes = s.getBytes(MarshallingConstants.charSet)
    int(s.length())
    oos.write(bytes, 0, bytes.length)
  }
}

/** The mirror image of the marshaller :) */
class Unmarshaller(val iis: InputStream) {
  val buf = new Array[Byte](4)

  def int : Int = {
    iis.read(buf, 0, 4)
    ((buf(3) & 0xff)
      + ((buf(2) & 0xff) << 8)
      + ((buf(1) & 0xff) << 16)
      + ((buf(0) & 0xff) << 24))
  }

  def string : String = {
    val length = int
    val bytes = new Array[Byte](length)
    iis.read(bytes, 0, length)
    new String(bytes, MarshallingConstants.charSet)
  }
}

object MarshallingConstants {
  val charSet = Charset.forName("UTF-8")
}