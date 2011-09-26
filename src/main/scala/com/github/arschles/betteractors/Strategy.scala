//some of this stolen from scalaz (https://github.com/scalaz/scalaz/blob/master/core/src/main/scala/scalaz/concurrent/Strategy.scala)
//thank you guys

package com.github.arschles.betteractors

import java.util.concurrent
import concurrent.{Executors, Callable, TimeUnit, ScheduledExecutorService}
import concurrent.{ExecutorService => JExecutorService, ScheduledExecutorService => JScheduledExecutorService}

//strategy represents the way that functions are computed. if a Function0 is the "what", then strategy is the "how".
//given a Function0[T], Strategy.apply returns a Function0[T] that, when executed, returns the result as computed by the strategy.
//in concrete terms, this means that when Strategy.apply is called, the strategy can decide to schedule a computation (or not), and then return a function
//that, when called, gets the result of the computation (meaning it blocks until the computation is complete)
trait Strategy {
  def apply[T](a: => T): () => T
}

object Strategies {
  import Strategy.lazyValToCallable

  implicit val numThreadsInScheduledThreadPool = 10
  implicit val executorService:JExecutorService = Executors.newCachedThreadPool
  implicit val scheduledExecutorService:JScheduledExecutorService = Executors.newScheduledThreadPool(numThreadsInScheduledThreadPool)

	class Synchronous extends Strategy {
		override def apply[T](a: => T) = {
			() => a
		}
	}

	class ExecutorService(implicit executorService:JExecutorService) extends Strategy {
		override def apply[T](a: => T) = {
      lazy val callable:Callable[T] = a
			val future = executorService.submit(callable)
			() => future.get
		}
	}
	
	class Delayed(delayMS:Long)(implicit scheduledExecutorService:ScheduledExecutorService) extends Strategy {

		override def apply[T](a: => T) = {
      lazy val callable:Callable[T] = a
      val future = scheduledExecutorService.schedule(callable, delayMS, TimeUnit.MILLISECONDS)
      () => future.get
    }
	}
}

object Strategy {
  implicit def lazyValToCallable[T](f: => T):Callable[T] = new Callable[T] {
    override def call = f
  }
}