package com.evrl.underground.perf

import bb.util.Benchmark
import com.evrl.underground.{IncomingMessage, BasicSequentialFilePersistence}

/**
 * Thus far, just a quick test to see how the bb benchmark library works in Scala. Some library
 * glue to be expected :)
 */
object BasicSequentialFilePerformance extends App {
  val iterations = 100000
  val task = new Runnable {
    def run {
      val persistence = BasicSequentialFilePersistence.onRandomDirectory
      val message = new IncomingMessage("hello, world, how are you".getBytes) // 25 bytes if you want to calculate throughput
      for (i <- 0 until iterations) {
        persistence.persist(message)
      }
      persistence.destroyData
    }
  }

  System.out.println("BasicSequentialFilePerformance: \n" + new Benchmark(task, iterations).toStringFull)
}