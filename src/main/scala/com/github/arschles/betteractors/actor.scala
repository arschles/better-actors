/**
* goals:
* type safety
* ability for actors to respond (no Unit returns)
* ability to send messages that get processed delayed
*/

package com.github.arschles.betteractors

import java.util.concurrent.{DelayQueue, Delayed, CountDownLatch, TimeUnit}

abstract class Operation[In, Out] {
	def apply(in:In):Out
}

sealed class Actor[In, Out](f:Operation[In, Out]) {
	import Strategies._	
	
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
		implicit val strategy = new DelayedStrategy(delayMS)
		{ f(a) }.future
	}
	
	/**
	* send an input to this actor and call countDown on the given CountDownLatch when the operation on this actor is complete
	*/
	def send(a:In, latch:CountDownLatch) {
		
		implicit val strategy = new ExecutorService
		{
			f(a)
			latch.countDown
		}.future
	}
	
	//compose this actor with a new actor. returns an actor which forwards the result of this to actor2
	def |||[Out2](actor2:Actor[Out, Out2]):Actor[In, Out2] = (in:In) => {
		val future1:Future[Out] = this ! in
		val future2:Future[Out2] = future1 ||| ((data:Out) => actor2 ! data)
		future2.get
	}.actor
}

object Actor {
	class ActorPimp[In, Out](fn:In => Out) {
		def actor = new Actor(fn)
	}
	implicit def funcToActorPimp[In, Out](f:In => Out) = new ActorPimp(f)
	
	def actor[In, Out](fn:In => Out) = new Actor(fn)
	
	implicit def funcToOperation[In, Out](f:In => Out) = new Operation[In, Out] {
		override def apply(in:In) = f(in)
	}
}