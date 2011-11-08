package com.evrl.underground.perf

import bb.util.Benchmark
import com.evrl.underground.{IncomingMessage, SequentialFilePersistence}

/**
 * Thus far, just a quick test to see how the bb benchmark library works in Scala. Some library
 * glue to be expected :)
 */
object SequentialFilePerformance extends App {
  val iterations = 100000
  val task = new Runnable {
    def run {
      val persistence = SequentialFilePersistence.onRandomDirectory
      val message = IncomingMessage("hello, world, how are you") // 25 bytes if you want to calculate throughput
      for (i <- 0 until iterations) {
        persistence.persist(message)
      }
      persistence.destroyData
    }
  }

  System.out.println("SequentialFilePerformance: \n" + new Benchmark(task, iterations).toStringFull)
}
