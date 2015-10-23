package com.rallyhealth.util.neo4j.journal.mongo

import com.mongodb.ConnectionString
import com.rallyhealth.util.neo4j.journal.{EventJournal, JournalEventCommitted, JournalEventResult}
import org.mongodb.scala._
import org.mongodb.scala.connection._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class MongoEventJournal(collection: MongoCollection[Document]) extends EventJournal[Document] {

  override def journal[Event](event: Event)(implicit converter: Event => Document): Future[JournalEventResult] = {
    Try(converter(event)) match {
      case Success(document) =>
        val observer = SingleResultObserver.of[Completed]
        collection.insertOne(document).subscribe(observer)
        observer.future.map(_ => JournalEventCommitted)
      case Failure(ex) =>
        Future.failed(ex)
    }
  }
}
object MongoEventJournal {

  // Close the client after some time and reconnect automatically
  private[this] var _client: MongoClient = null

  def maybeClient: Option[MongoClient] = Option(_client)

  def close(): Unit = maybeClient.foreach(_.close())

  def fromConfig(implicit config: MongoJournalConfig): MongoEventJournal = {
    if (_client eq null) {
      _client = MongoClient(MongoClientSettings.builder()
        .clusterSettings(ClusterSettings.builder()
          .applyConnectionString(new ConnectionString(config.url)).build()
        ).build()
      )
    }
    val db = _client.getDatabase(config.dbName)
    val journal = db.getCollection(config.collectionName)
    new MongoEventJournal(journal)
  }
}

case class MongoJournalConfig(url: String, dbName: String, collectionName: String)
