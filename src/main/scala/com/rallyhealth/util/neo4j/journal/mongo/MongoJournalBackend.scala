package com.rallyhealth.util.neo4j.journal.mongo

import com.rallyhealth.util.neo4j.journal.{EventJournal, JournalBackend}

trait MongoJournalBackend extends JournalBackend {
  self: HasMongoJournalConfig =>

  override def journal: EventJournal = MongoEventJournal.fromConfig(config)
}

trait HasMongoJournalConfig {
  def config: MongoJournalConfig
}

trait PropertiesBasedJournalConfig extends HasMongoJournalConfig {
  override def config: MongoJournalConfig = PropertiesBasedJournalConfig.config
}
object PropertiesBasedJournalConfig {

  lazy val config: MongoJournalConfig = {
    val journalBackend = sys.props.get("neo4j.journal.backend").getOrElse("mongo")
    require(
      journalBackend == "mongo",
      s"Cannot create Mongo backend. Configured journal is $journalBackend not 'mongo'")
    val foundDbName = sys.props.get("neo4j.journal.mongo.dbName")
    require(foundDbName.isDefined, "The dbName Java property is required to use properties to start the app.")
    val collectionName = sys.props.get("neo4j.journal.mongo.dbName").getOrElse("journal")
    require(foundDbName.isDefined, "The dbName Java property is required to use properties to start the app.")
    val connectionUrl = sys.props.get("neo4j.journal.mongo.url").getOrElse("http://localhost:28017")
    MongoJournalConfig(
      url = connectionUrl ,
      dbName = foundDbName.get,
      collectionName = collectionName
    )
  }
}
