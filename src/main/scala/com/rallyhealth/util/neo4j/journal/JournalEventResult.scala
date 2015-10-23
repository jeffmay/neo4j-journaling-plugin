package com.rallyhealth.util.neo4j.journal

sealed trait JournalEventResult {
  def success: Boolean
}

case object JournalEventCommitted extends JournalEventCommitted
sealed trait JournalEventCommitted extends JournalEventResult {
  override def success: Boolean = true
}

sealed trait JournalEventError extends JournalEventResult {
  override def success: Boolean = false
}

case object JournalTransientError extends JournalEventError
case object JournalClientError extends JournalEventError
case object JournalServerError extends JournalEventError
