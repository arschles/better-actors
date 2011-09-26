package com.github.arschles.betteractors

import java.util.concurrent.{CountDownLatch, LinkedBlockingQueue}

trait Operation[In, Out] {
	def apply(in:In):Out
}

sealed class Actor[In, Out](f:Operation[In, Out]) {
  import Actor._
	import Strategies._
  import Future._

	/**
	* equivalent to send(a, 0)
	*/
	def ! (a:In): Future[Out] = send(a, 0)
	
	/**
	* send an input to this actor, delay doing any operation on the input for a given amount of time
	* and return a future representing the result. the future will block on get for at least delayMS milliseconds
	* plus the execution time of the operation that this actor contains
	*/
	def send(a:In, delayMS:Long = 0): Future[Out] = {
		implicit val strategy = new Delayed(delayMS)
    lazy val op = f(a)
    future(op)
	}
	
	/**
	* send an input to this actor and call countDown on the given CountDownLatch when the operation on this actor is complete
	*/
	def send(a:In, latch:CountDownLatch) {
		implicit val strategy:Strategy = new ExecutorService
    lazy val op = {
			f(a)
			latch.countDown()
		}
    future(op)
	}
	
	//compose this actor with a new actor. return an actor which forwards the result of this to actor2
	def |||[Out2](actor2:Actor[Out, Out2]):Actor[In, Out2] = actor { in =>
    ((this ! in) ||| (data => actor2 ! data)).get
	}

  //compose this actor with a new function that returns a future. return an actor which forwards the result of this
  //to the given function
  def |||[Out2](fn:Out => Future[Out2]):Actor[In, Out2] = actor { in =>
    ((this ! in) ||| fn).get
  }
}

object Actor {
	class ActorPimp[In, Out](fn:In => Out) {
		def actor = new Actor(fn)
	}
	implicit def funcToActorPimp[In, Out](f:In => Out):ActorPimp[In, Out] = new ActorPimp(f)
	
	implicit def funcToOperation[In, Out](f:In => Out):Operation[In, Out] = new Operation[In, Out] {
		override def apply(in:In) = f(in)
	}

  def actor[In, Out](fn:In => Out) = new Actor(fn)
}