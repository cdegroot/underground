package com.evrl.underground.testutils

import com.evrl.underground.IncomingMessage
import org.hamcrest.{Description, TypeSafeMatcher}

class IncomingMessageMatcher(message: String) extends TypeSafeMatcher[IncomingMessage] {

  override def matchesSafely(msg: IncomingMessage) = sameString(msg.data, message)

  def sameString(array: Array[Byte], s: String) = s.equals(new String(array))

  override def describeTo(p1: Description) = "IncomingMessageMatcher on (" + message + ")"
}

object IncomingMessageMatcherFactory {
  def matchMessage(data: String) = new IncomingMessageMatcher(data)
}