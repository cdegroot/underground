package com.evrl.underground.testutils

import com.evrl.underground.BasicSequentialFilePersistence
import org.scalatest.{FunSuite, BeforeAndAfter}

class SuiteOnBasicSequentialFilePersistence extends FunSuite with BeforeAndAfter {
  val cycle = new JMockCycle

  var persistence : BasicSequentialFilePersistence = _
  var logFile : String = _

  before {
    persistence = BasicSequentialFilePersistence.onRandomDirectory
    logFile = persistence.baseDirName + "/message.log"
  }
  after {
    persistence.destroyData
  }
}