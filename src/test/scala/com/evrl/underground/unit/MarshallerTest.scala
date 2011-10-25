package com.evrl.underground.unit

import org.scalatest.FunSuite
import com.evrl.underground.testutils.JMockCycle
import java.io.{InputStream, OutputStream}
import com.evrl.underground.{Unmarshaller, Marshaller}
import com.sun.xml.internal.ws.developer.MemberSubmissionAddressing.Validation
import com.sun.jdi.Value
import java.util.Arrays
import org.hamcrest.{Description, BaseMatcher, Matcher}

class MarshallerTest extends FunSuite {
  val cycle = new JMockCycle
  import cycle._

  test("marshalling int") {
    val os = mock[OutputStream]
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
    val is = mock[InputStream]
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
}