This is my attempt to build actors in Scala that have everything that I want in them, specifically the following:

* type safety
* all operations return a future
* delayed execution

Most of the inspiration for this was taken from Scalaz, specifically
[here](http://www.slideshare.net/bostonscala/beyond-mere-actors) and
[here](https://github.com/scalaz/scalaz/blob/master/core/src/main/scala/scalaz/concurrent/Promise.scala).

This code has a long way to go, so don't rely on it in production. or actually at all right now.