package com.github.arschles.betteractors

import java.util.concurrent.{Executors, Future => JFuture, Callable}

sealed class Future[T](fn: => T)(implicit val strategy:Strategy) {
	private val getResult = strategy(fn)
	
	def get:getResult
	
	//compose this future with a function
	def |||[U](fn:T => U):Future[U] = {
		lazy val newOperation = fn(get)
		import Strategies._
		implicit val strategy = if(done) { new Sequential } else { new ExecutorService }
		new Future(newOperation)
	}
}

object Future {
	class FuturePimp[T](a: => T)(implicit strategy:Strategy) {
		def future:Future[T] = new Future(a)
	}
	
	implicit def lazyValToFuturePimp(f: => T)(implicit strategy:Strategy) = new FuturePimp(f)
	
	def future(f: => T, implicit val strategy:Strategy) = new Future(f)
}