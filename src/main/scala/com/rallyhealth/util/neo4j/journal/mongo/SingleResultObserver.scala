package com.rallyhealth.util.neo4j.journal.mongo

import org.mongodb.scala.Observer

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

class SingleResultObserver[T] extends Observer[T] {
  private[this] val promise: Promise[T] = Promise[T]()
  override def onNext(result: T): Unit = promise.complete(Success(result))
  override def onError(e: Throwable): Unit = promise.complete(Failure(e))
  override def onComplete(): Unit = ()
  final def future: Future[T] = promise.future
}

object SingleResultObserver {
  def of[T]: SingleResultObserver[T] = new SingleResultObserver[T]
}