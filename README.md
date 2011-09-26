# What
This is my attempt to build actors in Scala that have everything that I want them to do. here's my list in no order:

1. type safety
2. all operations return a future
3. delayed execution
4. easily managed complex shared state

Most of the inspiration for this was taken from Scalaz, specifically
[here](http://www.slideshare.net/bostonscala/beyond-mere-actors) and
[here](https://github.com/scalaz/scalaz/blob/master/core/src/main/scala/scalaz/concurrent/Promise.scala).

This code has a long way to go, so don't rely on it in production. or actually at all right now. I have 1-3 done, but
4 is still in progress.


# How
It aims to be pretty simple. Here's an example (explicit types included for clarity):

```scala
import com.github.arschles.betteractors._
import Actor.actor

//the actor helper method takes in a Function1 and returns an actor that takes
// T and returns Future[T]
val doIncrementActor:Actor[Int, Int] = actor { in:Int => in + 1 }

//send operations to the actor and get back a com.github.arschles.Future, which is similar
//to java.util.concurrent.Future except it has functional composition which can be nice
val future:Future[Int] = doIncrementActor ! 22

//if you want to do something after the operation is done (ie: future.get stops blocking and returns),
//use |||. This is similar to the unix pipe concept - take the output of the future when ready and pass
//it to the given function. note that the given function can itself return a value or a Future. either way
//you get back a new future
val nextFuture:Future[String] = future ||| (intResult => intResult.toString + " as a string")

println(nextFuture) //"23 as a string"
```

# Todo
1. accomplish #4 above in the "What" section
2. write tests
3. goto 2