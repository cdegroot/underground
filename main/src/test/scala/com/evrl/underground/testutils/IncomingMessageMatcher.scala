package com.evrl.underground.testutils

import com.evrl.underground.IncomingMessage
import org.hamcrest.{Description, TypeSafeMatcher}
import org.scalatest.FunSuite
import java.util.Arrays

class IncomingMessageMatcher(message: Array[Byte]) extends TypeSafeMatcher[IncomingMessage] {

  override def matchesSafely(msg: IncomingMessage) = Arrays.equals(msg.data, message)

  override def describeTo(p1: Description) = "IncomingMessageMatcher on (" + message + ")"
}

object IncomingMessageMatcherFactory {
  def matchMessage(data: String) = new IncomingMessageMatcher(data.getBytes)

  def matchMessage(data: Array[Byte]) = new IncomingMessageMatcher(data)
}

class TestMessageMatcher extends FunSuite {

  test("basic matching") {
    val message = "Hello, world"
    val ba1 = message.getBytes
    val ba2 = message.getBytes
    val matcher = IncomingMessageMatcherFactory.matchMessage(ba1)
    assert(matcher.matchesSafely(new IncomingMessage(ba2)))
  }
}