package com.github.arschles.betteractors

sealed class Future[T](fn: => T)(implicit val strategy:Strategy) {
  import Future._
  import Strategies._

	private val getResult = strategy(fn)
	
	def get:T = getResult.apply()
	
	//compose this future with a function
	def |||[U](fn:T => Future[U]):Future[U] = {
		lazy val newOperation:Future[U] = fn(get)
    newOperation
	}

  def |||[U](fn:T => U):Future[U] = {
    lazy val newOperation:U = fn(get)
    future(newOperation)
  }
}

object Future {
	class FuturePimp[T](a: => T)(implicit strategy:Strategy) {
		def future:Future[T] = new Future(a)
	}
	
	implicit def lazyValToFuturePimp[T](f: => T)(implicit strategy:Strategy):FuturePimp[T] = new FuturePimp(f)
	
	def future[T](f: => T)(implicit strategy:Strategy):Future[T] = new Future(f)
}