package com.evrl.underground

import java.io.{OutputStream, InputStream}

/**
 * A very straightforward marshaller. This marshaller is, as you can see, decidedly not multi-threaded. This
 * is a design feature :)
 */
class Marshaller(val oos : OutputStream) {
  val buf = new Array[Byte](4)

  def int(value : Int) {
    buf(0) = (value >> 24).asInstanceOf[Byte];
    buf(1) = (value >> 16).asInstanceOf[Byte];
    buf(2) = (value >> 8).asInstanceOf[Byte];
    buf(3) = value.asInstanceOf[Byte]
    oos.write(buf, 0, 4)
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
}