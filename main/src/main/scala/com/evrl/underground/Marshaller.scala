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

  /**
   * Write an integer to the underlying stream. Encoding is always Big Endian.
   *
   * @param value the (32bit) integer value to write to the stream
   */
  def int(value: Int) {
    buf(0) = (value >> 24).asInstanceOf[Byte]
    buf(1) = (value >> 16).asInstanceOf[Byte]
    buf(2) = (value >> 8).asInstanceOf[Byte]
    buf(3) = value.asInstanceOf[Byte]
    oos.write(buf, 0, 4)
  }

  /**
   * Write a string to the underlying stream. Encoding is always UTF-8, the
   * string is prefixed by an integer length.
   *
   * @param s the string to write
   */
  def string(s: String) {
    val bytes = s.getBytes(MarshallingConstants.charSet)
    int(s.length())
    oos.write(bytes, 0, bytes.length)
  }

  /**
   * Write a byte array to the underlying stream. The byte array is prefixed
   * by an integer length.
   *
   * @param bytes the byte array to write
   */
  def bytes(bytes: Array[Byte]) {
    int(bytes.length)
    oos.write(bytes, 0, bytes.length)
  }
}

/** The mirror image of the marshaller :) */
class Unmarshaller(val iis: InputStream) {

  val buf = new Array[Byte](4)

  /**
   * Unmarshall a 32bit integer. Consumes four bytes in Big Endian order.
   * @return a 32 bit integer.
   */
  def int: Int = {
    iis.read(buf, 0, 4)
    ((buf(3) & 0xff)
      + ((buf(2) & 0xff) << 8)
      + ((buf(1) & 0xff) << 16)
      + ((buf(0) & 0xff) << 24))
  }

  /**
   * Unmarshall a string. Will unmarshall a byte array (@see #bytes) and then
   * return the result as an interpretation through UTF8 decoding.
   * @return a string
   */
  def string: String = {
    new String(bytes, MarshallingConstants.charSet)
  }

  /**
   * Unmarshall a byte array. First reads 32bits of length and then the number
   * of bytes representing the length.
   * @return a byte array
   */
  def bytes: Array[Byte] = {
    val length = int
    val bytes = new Array[Byte](length)
    iis.read(bytes, 0, length)
    bytes
  }
}

object MarshallingConstants {
  val charSet = Charset.forName("UTF-8")
}