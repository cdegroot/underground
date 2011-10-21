package com.evrl.underground.testutils

/*
 * Copyright 2001-2009 Artima, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.scalatest._
import org.jmock.api.ExpectationError
import org.jmock.Expectations
import org.jmock.Mockery
import org.jmock.{Expectations, Mockery}
import org.jmock.lib.legacy.ClassImposteriser
import scala.reflect.Manifest
import org.jmock.lib.legacy.ClassImposteriser
import org.scalatest.mock.JMockExpectations

// Blatantly stolen from ScalaTest to expose context, etcetera :)
final class JMockCycle {

  val context = new Mockery
  context.setImposteriser(ClassImposteriser.INSTANCE)


  def mock[T <: AnyRef](implicit manifest: Manifest[T]): T = {
    context.mock(manifest.erasure.asInstanceOf[Class[T]])
  }


  def expecting(fun: JMockExpectations => Unit) {
    val e = new JMockExpectations
    fun(e)
    context.checking(e)
  }


  def whenExecuting(fun: => Unit) = {
    fun
    context.assertIsSatisfied()
  }
}
