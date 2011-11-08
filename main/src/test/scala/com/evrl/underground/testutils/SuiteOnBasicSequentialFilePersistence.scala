package com.evrl.underground.testutils

import com.evrl.underground.SequentialFilePersistence
import org.scalatest.{FunSuite, BeforeAndAfter}

class SuiteOnBasicSequentialFilePersistence extends FunSuite with BeforeAndAfter {
  val cycle = new JMockCycle

  var persistence : SequentialFilePersistence = _
  var logFileBase : String = _

  before {
    persistence = SequentialFilePersistence.onRandomDirectory
    logFileBase = persistence.baseDirName + "/message.log."
  }
  after {
    persistence.destroyData
  }
}