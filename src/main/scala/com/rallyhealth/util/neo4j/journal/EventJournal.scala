package com.rallyhealth.util.neo4j.journal

import scala.concurrent.Future

trait EventJournal[StorageFormat] {

  def journal[Event](event: Event)(implicit converter: Event => StorageFormat): Future[JournalEventResult]
}
