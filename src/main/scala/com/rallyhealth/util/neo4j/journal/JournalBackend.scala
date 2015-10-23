package com.rallyhealth.util.neo4j.journal

private[journal] trait JournalBackend {

  def journal: EventJournal
}
