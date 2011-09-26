package com.github.arschles.betteractors

import java.util.concurrent.{DelayQueue, Delayed, Exchanger, TimeUnit, TimeoutException}
import java.lang.Thread

trait Operation[In, Out] {
	def apply(in:In):Out

  //return a new operation that represents the result of passing the result of this operation
  //to the new operation
  def andThen[Out2](nextOp:Operation[Out, Out2]):Operation[In, Out2] = {
    val firstOp = this
    new Operation[In, Out2] {
      override def apply(in:In):Out2 = nextOp(firstOp(in))
    }
  }

  def |||[Out2](nextOp:Operation[Out, Out2]):Operation[In, Out2] = andThen(nextOp)
}

sealed class Actor[In, Out](f:Operation[In, Out]) {
  import Actor._
  import Strategies.Synchronous
  import Future._

  //the synchronous strategy for futures means that we'll waste no background threads just waiting on the Exchanger
  implicit private val strategy = new Synchronous

  private case class DelayedPayload(input:In, delayMS:Long) extends Delayed {
    val exchanger = new Exchanger[Option[Out]]
    def getDelay(unit:TimeUnit) = unit.convert(delayMS, TimeUnit.MILLISECONDS)
    def compareTo(delayed:Delayed) = getDelay(TimeUnit.MILLISECONDS).compareTo(delayed.getDelay(TimeUnit.MILLISECONDS))
  }

  private val queue = new DelayQueue[DelayedPayload]

  private val thread = new Thread {
    override def run() {
      val payload = queue.take
      payload.exchanger.exchange(Some(f(payload.input)), 1, TimeUnit.SECONDS)
      run()
    }
  }
  thread.start()

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
    val payload = new DelayedPayload(a, delayMS)
    lazy val op = {
      try {
        payload.exchanger.exchange(None, 1, TimeUnit.SECONDS).get
      }
      catch {
        case e:TimeoutException =>
          throw new TimeoutException("failed to get result for input " + a + " after 1 second (" + e.getMessage + ")")
      }
    }
    queue.put(payload)
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