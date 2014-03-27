package com.evrl.underground.unit

import org.scalatest.FunSuite
import com.evrl.underground.testutils.JMockCycle
import com.evrl.underground.{Unmarshaller, Marshaller}
import org.hamcrest.{Description, BaseMatcher}
import java.io.{ByteArrayInputStream, InputStream, OutputStream}

class MarshallerTest extends FunSuite {
  val cycle = new JMockCycle
  import cycle._

  test("marshalling int") {
    val os = context.mock(classOf[OutputStream], "streamForIntMarshall")
    val ms = new Marshaller(os)
    val marshalled = Array[Byte](0x12, 0x34, 0x56, -50)
    expecting { e => import e._
      oneOf(os).write(marshalled, 0, 4)
    }
    whenExecuting {
      ms.int(0x123456ce)
    }
  }

  test("unmarshalling int") {
    val is = context.mock(classOf[InputStream], "streamForIntUnmarshall")
    val ms = new Unmarshaller(is)
    val marshalled = Array[Byte](0x12, 0x34, 0x56, -50)
    val arrayWriter = new BaseMatcher[Array[Byte]] {
      def matches(value : Any) : Boolean = {
        System.arraycopy(marshalled, 0, value, 0, 4)
        true
      }
      def describeTo(description: Description) = description.appendText("Fake array setter")
    }
    expecting { e => import e._
      oneOf(is).read(`with`(arrayWriter), `with`(0), `with`(4))
    }
    whenExecuting {
      val answer = ms.int
      assert(answer == 0x123456ce, "Answer 0x" + answer.toHexString + " not expected")
    }
  }

  test("marshalling string") {
    val os = context.mock(classOf[OutputStream], "streamForStringMarshall")
    val ms = new Marshaller(os)
    val marshalledLength = Array[Byte](0, 0, 0, 12)
    val marshalledBytes = Array[Byte](72, 101, 108, 108, 111, 44, 32, 119, 111, 114, 108, 100)
    expecting { e => import e._
      oneOf(os).write(marshalledLength, 0, marshalledLength.length)
      oneOf(os).write(marshalledBytes, 0, marshalledBytes.length)
    }
    whenExecuting {
      val answer = ms.string("Hello, world")
    }
  }

  test("unmarshalling string") {
    val marshalled = Array[Byte](0, 0, 0, 12, 72, 101, 108, 108, 111, 44, 32, 119, 111, 114, 108, 100)
    val is = new ByteArrayInputStream(marshalled)
    val ms = new Unmarshaller(is)

    val string = ms.string
    assert("Hello, world".equals(string))
  }
}