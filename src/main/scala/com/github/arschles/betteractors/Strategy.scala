//some of this stolen from scalaz (https://github.com/scalaz/scalaz/blob/master/core/src/main/scala/scalaz/concurrent/Strategy.scala)

package com.github.arschles.betteractors

import java.util.concurrent.{Executors, Callable, TimeUnit, ExecutorService, ScheduledExecutorService, TimeUnit}

//strategy represents the way that functions are computed. if a Function0 is the "what", then strategy is the "how".
//given a Function0[T], Strategy.apply returns a Function0[T] that, when executed, returns the result as computed by the strategy.
//in concrete terms, this means that when Strategy.apply is called, the strategy can decide to schedule a computation (or not), and then return a function
//	that, when called, blocks until the computation is complete
trait Strategy {
  def apply[T](a: => T): () => T
}

object Strategies {
  implicit lazy val numThreadsInScheduledThreadPool = 10
	implicit lazy val defaultExecutorService = Executors.newCachedThreadPool
  implicit lazy val defaultScheduledExecutorService = Executors.newScheduledThreadPool(numThreadsInScheduledThreadPool)
	
	//blocks until all operations are complete and then returns a function that returns a future
	class Synchronous extends Strategy {
		override def apply[T](a: => T) = {
			() => a
		}
	}

	class ExecutorService(implicit s:ExecutorService) extends Strategy {
		override def apply[T](a: => T) = {
			val future = executorService.submit(new Callable[T] {
				override def call = a
			})
			() => future.get
		}
	}
	
	//TODO: implement the waiting inside a single thread waiting on a DelayQueue instead of one thread (in ExecutorService) per waiter
	class Delayed(delayMS:Long)(implicit s:ScheduledExecutorService) extends Strategy {

		override def apply[T](a: => T) = {
      val future = s.schedule(new Callable[T] {
        override def call = a
      }, delayMS, TimeUnit.MILLISECONDS)
      () => future.get
    }
	}
}