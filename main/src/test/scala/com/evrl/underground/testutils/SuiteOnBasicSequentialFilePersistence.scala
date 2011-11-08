package com.evrl.underground.testutils

import com.evrl.underground.SequentialFilePersistence
import org.scalatest.{FunSuite, BeforeAndAfter}

class SuiteOnBasicSequentialFilePersistence extends FunSuite with BeforeAndAfter {
  val cycle = new JMockCycle

  var persistence : SequentialFilePersistence = _
  var logFile : String = _

  before {
    persistence = SequentialFilePersistence.onRandomDirectory
    logFile = persistence.baseDirName + "/message.log"
  }
  after {
    persistence.destroyData
  }
}